package com.zhiwei.math.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.data.repo.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(private val repo: ChatRepository) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> =
        repo.observeConversations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun create(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.createConversation()) }
    }

    fun rename(id: Long, title: String) {
        viewModelScope.launch { repo.renameConversation(id, title) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteConversation(id) }
    }
}
