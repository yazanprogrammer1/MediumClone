package com.example.mediumclone.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.example.mediumclone.data.service.GenerativeAIService
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val draftsRepository: com.example.mediumclone.data.repository.DraftsRepository,
    private val generativeAIService: com.example.mediumclone.data.service.GenerativeAIService,
    private val subscriptionRepository: com.example.mediumclone.data.repository.SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: String? = savedStateHandle["articleId"]
    private val draftIdArg: String? = savedStateHandle["draftId"]

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow(TextFieldValue(""))
    val content: StateFlow<TextFieldValue> = _content.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _existingImageUrl = MutableStateFlow<String?>(null)
    val existingImageUrl: StateFlow<String?> = _existingImageUrl.asStateFlow()

    private var currentArticleId: String? = null
    private var currentDraftId: String = ""
    private var isDraftLoaded = false

    // AI Loading State
    private val _isGeneratingAI = MutableStateFlow(false)
    val isGeneratingAI: StateFlow<Boolean> = _isGeneratingAI.asStateFlow()
    
    // AI Gating State
    private val _showPremiumDialog = MutableStateFlow(false)
    val showPremiumDialog: StateFlow<Boolean> = _showPremiumDialog.asStateFlow()
    
    private var hasUnlockedAI = false
    
    // Autosave Status
    private val _autosaveStatus = MutableStateFlow<String?>("Saved")
    val autosaveStatus: StateFlow<String?> = _autosaveStatus.asStateFlow()

    init {
        if (articleId != null) {
            loadArticle(articleId)
        } else {
            // Draft Logic
            if (draftIdArg != null) {
                currentDraftId = draftIdArg
                loadDraft(draftIdArg)
            } else {
                // New Draft
                currentDraftId = java.util.UUID.randomUUID().toString()
                isDraftLoaded = true // Ready to save immediately
            }
        }
        
        startAutosave()
    }
    
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun startAutosave() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_title, _content, _tags, _existingImageUrl) { t, c, tags, img ->
                Quad(t, c.text, tags, img)
            }
            .debounce(1000)
            .collect { (t, c, tags, img) ->
                // Guard: Only autosave if we are in Draft mode AND the draft is loaded/initialized.
                // This prevents overwriting a draft with empty initial state before it loads from DB.
                if (currentArticleId == null && isDraftLoaded && (t.isNotBlank() || c.isNotBlank())) {
                    _autosaveStatus.value = "Saving..."
                    saveDraftLocal(t, c, tags, img)
                    _autosaveStatus.value = "Saved"
                }
            }
        }
    }

    private suspend fun saveDraftLocal(t: String, c: String, tags: List<String>, img: String?) {
        val draft = com.example.mediumclone.data.local.Draft(
            id = currentDraftId,
            title = t,
            content = c,
            coverImageUrl = img,
            tags = tags.joinToString(","),
            lastModified = System.currentTimeMillis()
        )
        draftsRepository.saveDraft(draft)
    }

    // Helper data class for combine
    data class Quad(val t: String, val c: String, val tags: List<String>, val img: String?)
    
    private fun loadDraft(draftId: String) {
         viewModelScope.launch {
            val draft = draftsRepository.getDraft(draftId)
            if (draft != null) {
                _title.value = draft.title
                _content.value = TextFieldValue(draft.content)
                _tags.value = if (draft.tags.isBlank()) emptyList() else draft.tags.split(",")
                _existingImageUrl.value = draft.coverImageUrl
            }
            isDraftLoaded = true // Mark as safe to autosave
         }
    }
    
    fun dismissPremiumDialog() {
        _showPremiumDialog.value = false
    }
    
    fun unlockAITemporarily() {
        hasUnlockedAI = true
        _showPremiumDialog.value = false
    }
    
    fun generateAIContent(topic: String) {
        viewModelScope.launch {
            if (!subscriptionRepository.getSubscriptionStatus() && !hasUnlockedAI) {
                _showPremiumDialog.value = true
                return@launch
            }
            
            _isGeneratingAI.value = true
            try {
                val generatedText = generativeAIService.generateArticleContent(topic)
                val currentText = _content.value.text
                val newText = if (currentText.isBlank()) generatedText else "$currentText\n\n$generatedText"
                _content.value = TextFieldValue(newText)
                hasUnlockedAI = false // Reset after use if desired, or keep open for session. Let's consume it.
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = EditorUiState.Error("AI Error: ${e.message}")
            } finally {
                _isGeneratingAI.value = false
            }
        }
    }

    private val _isUploadingInlineImage = MutableStateFlow(false)
    val isUploadingInlineImage: StateFlow<Boolean> = _isUploadingInlineImage.asStateFlow()

    fun uploadInlineImage(imageBytes: ByteArray, cursorPosition: Int) {
        viewModelScope.launch {
            _isUploadingInlineImage.value = true
            val result = articlesRepository.uploadImage(imageBytes)
            _isUploadingInlineImage.value = false
            
            result.onSuccess { url ->
                val markdown = "\n![Image]($url)\n"
                insertTextAtCursor(markdown, cursorPosition)
            }.onFailure {
                _uiState.value = EditorUiState.Error("Image upload failed: ${it.message}")
            }
        }
    }

    private fun insertTextAtCursor(textToInsert: String, cursorPosition: Int) {
        val currentTF = _content.value
        val currentText = currentTF.text
        
        val safeCursor = cursorPosition.coerceIn(0, currentText.length)
        
        val newText = StringBuilder(currentText)
            .insert(safeCursor, textToInsert)
            .toString()
            
        val newCursorPos = safeCursor + textToInsert.length
        
        _content.value = currentTF.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursorPos)
        )
    }

    fun loadArticle(articleId: String) {
        if (currentArticleId == articleId) return // Already loaded
        
        currentArticleId = articleId
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            val result = articlesRepository.getArticle(articleId)
            
            if (result.isSuccess) {
                val article = result.getOrThrow()
                _title.value = article.title
                _content.value = TextFieldValue(article.content)
                _tags.value = article.tags
                _existingImageUrl.value = article.coverImageUrl
                _uiState.value = EditorUiState.Idle
            } else {
                _uiState.value = EditorUiState.Error("Failed to load article")
            }
        }
    }
    


    fun updateTitle(newTitle: String) { _title.value = newTitle }
    fun updateContent(newContent: TextFieldValue) { _content.value = newContent }
    fun updateTags(newTags: List<String>) { _tags.value = newTags }
    fun addTag(tag: String) { 
        if (_tags.value.size < 5 && !_tags.value.contains(tag)) {
            _tags.value = _tags.value + tag
        }
    }
    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    fun publishArticle(imageBytes: ByteArray?) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            
            var imageUrl: String? = _existingImageUrl.value
            if (imageBytes != null) {
                val uploadResult = articlesRepository.uploadImage(imageBytes)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                } else {
                    _uiState.value = EditorUiState.Error("Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
                    return@launch
                }
            }

            val result = if (currentArticleId != null) {
                articlesRepository.updateArticle(currentArticleId!!, _title.value, _content.value.text, imageUrl, _tags.value)
            } else {
                articlesRepository.createArticle(_title.value, _content.value.text, imageUrl, _tags.value)
            }

            if (result.isSuccess) {
                // If success and we were working on a draft, delete it
                if (currentArticleId == null) {
                    draftsRepository.deleteDraft(currentDraftId)
                }
                _uiState.value = EditorUiState.Success
            } else {
                _uiState.value = EditorUiState.Error(result.exceptionOrNull()?.message ?: "Failed to publish")
            }
        }
    }

    fun resetState() {
        _uiState.value = EditorUiState.Idle
        _title.value = ""
        _content.value = TextFieldValue("")
        _tags.value = emptyList()
        _existingImageUrl.value = null
        currentArticleId = null
        // Generate new draft ID for next session
        currentDraftId = java.util.UUID.randomUUID().toString()
    }

}

sealed class EditorUiState {
    object Idle : EditorUiState()
    object Loading : EditorUiState()
    object Success : EditorUiState()
    data class Error(val message: String) : EditorUiState()
}
