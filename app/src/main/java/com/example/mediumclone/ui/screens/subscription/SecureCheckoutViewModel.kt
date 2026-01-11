package com.example.mediumclone.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecureCheckoutViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun processPayment(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            // Simulate network delay and verification
            delay(3000) 
            // Upgrade user
            subscriptionRepository.upgradeToPremium()
            _isProcessing.value = false
            onSuccess()
        }
    }
}
