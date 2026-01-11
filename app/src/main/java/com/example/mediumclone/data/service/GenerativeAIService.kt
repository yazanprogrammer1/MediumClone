package com.example.mediumclone.data.service

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface GenerativeAIService {
    suspend fun generateArticleContent(topic: String): String
}

@Singleton
class MockGenerativeAIService @Inject constructor() : GenerativeAIService {
    override suspend fun generateArticleContent(topic: String): String {
        // Simulate network delay
        delay(3000)
        
        return """
            # The Future of $topic
            
            By *AI Assistant*
            
            > "Innovation involves finding a new way to do things." 
            
            In recent years, **$topic** has emerged as a transformative force. Experts believe that understanding the nuances of this subject is crucial for navigating the modern landscape.
            
            ## Key Concepts
            
            When we talk about **$topic**, we usually refer to a few core principles:
            
            - **Efficiency**: Doing more with less.
            - **Scalability**: Growing without breaking.
            - **Sustainability**: Lasting for the long haul.
            
            ## Code Example
            
            Here is how you might represent $topic in code:
            
            ```kotlin
            fun explore(subject: String) {
                val potential = calculatePotential(subject)
                println("The future of ${"$"}subject is bright!")
            }
            ```
            
            ## Conclusion
            
            As we look ahead, it is clear that **$topic** will continue to shape our world. Whether you are a beginner or an expert, staying informed is key. 
            
            [Learn more about $topic](https://google.com/search?q=$topic)
        """.trimIndent()
    }
}
