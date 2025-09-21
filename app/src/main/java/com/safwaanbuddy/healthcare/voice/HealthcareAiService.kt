// Healthcare AI Service
// Provides conversational AI features for the healthcare app

package com.safwaanbuddy.healthcare.voice

import android.content.Context
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HealthcareAiService @Inject constructor(
    private val context: Context,
    private val openAiApiKey: String,
    private val geminiApiKey: String
) {
    
    companion object {
        private const val TAG = "HealthcareAiService"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
        private const val DEFAULT_GEMINI_MODEL = "gemini-pro"
    }
    
    // Initialize OpenAI client
    private val openAI = OpenAI(openAiApiKey)
    
    // Initialize Gemini client
    private val geminiModel = GenerativeModel(DEFAULT_GEMINI_MODEL, geminiApiKey)
    private val geminiModelFutures = GenerativeModelFutures(geminiModel)
    
    // Create a thread pool for AI requests
    private val aiExecutor = Executors.newSingleThreadExecutor()
    
    // Healthcare persona for AI responses
    private val healthcarePrompt = """
        You are SafwaanBuddy, a caring healthcare companion for pregnant women.
        
        You should always respond with:
        - Empathy and compassion
        - Healthcare knowledge
        - Proactive check-ins
        - Gentle reminders
        - Motivational messages
        
        Your responses should be:
        - Personalized
        - Supportive
        - Encouraging
        - Healthcare-focused
        - In a friendly, caring tone
        
        You should:
        - Ask how the user is feeling
        - Check if they took their medication
        - Celebrate their progress
        - Provide gentle reminders
        - Follow up on previous interactions
        
        You should not:
        - Be overly technical
        - Give medical advice beyond your training
        - Ignore the emotional aspects of healthcare
        - Be impersonal or robotic
        - Forget previous interactions
        
        Current date: ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}
        """
    
    /**
     * Get a conversational response from OpenAI
     * @param input The user's input
     * @param patientId The ID of the current patient
     * @return A listenable future with the AI response
     */
    fun getOpenAIResponse(input: String, patientId: Long): ListenableFuture<String> {
        // Create a prompt with healthcare context
        val healthcareInput = """
            $healthcarePrompt
            
            User: $input
            
            Assistant: 
            """
        
        // Create a future for the response
        val responseFuture = CompletableFuture<String>()
        
        // Make the API call in the background
        aiExecutor.execute {
            try {
                val messages = listOf(
                    ChatMessage(role = ChatRole.SYSTEM, content = healthcarePrompt),
                    ChatMessage(role = ChatRole.USER, content = input)
                )
                
                val chatCompletion = openAI.chatCompletion(
                    model = ModelId(DEFAULT_MODEL),
                    messages = messages,
                    temperature = 0.7f,
                    maxTokens = 200
                )
                
                val response = chatCompletion.choices.first().message?.content ?: ""
                responseFuture.complete(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting OpenAI response", e)
                responseFuture.completeExceptionally(e)
            }
        }
        
        return responseFuture.asFuture()
    }
    
    /**
     * Get a conversational response from Gemini
     * @param input The user's input
     * @param patientId The ID of the current patient
     * @return A listenable future with the AI response
     */
    fun getGeminiResponse(input: String, patientId: Long): ListenableFuture<String> {
        // Create a prompt with healthcare context
        val healthcareInput = """
            $healthcarePrompt
            
            User: $input
            
            Assistant: 
            """
        
        // Create a future for the response
        val responseFuture = CompletableFuture<String>()
        
        // Make the API call in the background
        aiExecutor.execute {
            try {
                val future = geminiModelFutures.generateContent(
                    listOf(ChatMessage.create(healthcareInput))
                )
                
                Futures.addCallback(future, object : FutureCallback<String> {
                    override fun onSuccess(result: String?) {
                        responseFuture.complete(result ?: "")
                    }
                    
                    override fun onFailure(t: Throwable) {
                        Log.e(TAG, "Error getting Gemini response", t)
                        responseFuture.completeExceptionally(t)
                    }
                }, aiExecutor)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting Gemini response", e)
                responseFuture.completeExceptionally(e)
            }
        }
        
        return responseFuture.asFuture()
    }
    
    /**
     * Process a healthcare query with personality and empathy
     * @param input The user's input
     * @param patientId The ID of the current patient
     * @param useOpenAI Whether to use OpenAI (true) or Gemini (false)
     * @return A listenable future with the empathetic healthcare response
     */
    fun processHealthcareQuery(input: String, patientId: Long, useOpenAI: Boolean = true): ListenableFuture<String> {
        // Add healthcare context and personality to the query
        val healthcareInput = """
            You are SafwaanBuddy, a caring healthcare companion for pregnant women.
            
            You should always respond with empathy, compassion, and healthcare knowledge.
            
            Current date: ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}
            
            $input
            """
        
        return if (useOpenAI) {
            getOpenAIResponse(healthcareInput, patientId)
        } else {
            getGeminiResponse(healthcareInput, patientId)
        }
    }
    
    /**
     * Generate a proactive health check-in message
     * @param patientId The ID of the current patient
     * @param useOpenAI Whether to use OpenAI (true) or Gemini (false)
     * @return A listenable future with the check-in message
     */
    fun generateProactiveCheckIn(patientId: Long, useOpenAI: Boolean = true): ListenableFuture<String> {
        val checkInPrompt = """
            You are SafwaanBuddy, a caring healthcare companion for pregnant women.
            
            Generate a personalized, empathetic health check-in message for the user.
            
            Ask about their well-being, remind them to take medication, and offer encouragement.
            
            Current date: ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}
            """
        
        return if (useOpenAI) {
            getOpenAIResponse(checkInPrompt, patientId)
        } else {
            getGeminiResponse(checkInPrompt, patientId)
        }
    }
    
    /**
     * Generate a motivational message
     * @param patientId The ID of the current patient
     * @param useOpenAI Whether to use OpenAI (true) or Gemini (false)
     * @return A listenable future with the motivational message
     */
    fun generateMotivationalMessage(patientId: Long, useOpenAI: Boolean = true): ListenableFuture<String> {
        val motivationPrompt = """
            You are SafwaanBuddy, a caring healthcare companion for pregnant women.
            
            Generate a personalized, motivational message for the user.
            
            Celebrate their progress, offer encouragement, and provide emotional support.
            
            Current date: ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}
            """
        
        return if (useOpenAI) {
            getOpenAIResponse(motivationPrompt, patientId)
        } else {
            getGeminiResponse(motivationPrompt, patientId)
        }
    }
    
    /**
     * Generate a follow-up question
     * @param patientId The ID of the current patient
     * @param previousQuery The previous user query
     * @param useOpenAI Whether to use OpenAI (true) or Gemini (false)
     * @return A listenable future with the follow-up question
     */
    fun generateFollowUpQuestion(patientId: Long, previousQuery: String, useOpenAI: Boolean = true): ListenableFuture<String> {
        val followUpPrompt = """
            You are SafwaanBuddy, a caring healthcare companion for pregnant women.
            
            Generate a follow-up question based on the user's previous query: "$previousQuery"
            
            The question should be:
            - Personalized
            - Supportive
            - Relevant to the previous query
            - In a caring, friendly tone
            
            Current date: ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}
            """
        
        return if (useOpenAI) {
            getOpenAIResponse(followUpPrompt, patientId)
        } else {
            getGeminiResponse(followUpPrompt, patientId)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        aiExecutor.shutdown()
    }
}