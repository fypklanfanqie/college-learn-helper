package com.zhiwei.math.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 对话（一门科目一个学习线程） */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** 科目：V1 固定 "math"（高等数学） */
    val subject: String = "math",
    /** 模式：DETAIL / EXAM_SPRINT */
    val mode: String = "DETAIL",
    /** Layer 2 定制提示词段（元提示词生成，字节级存库；空 = 未设置） */
    val layer2Prompt: String = "",
    /** 四项对话设置原文（用于重新生成 Layer 2） */
    val examFocus: String = "",
    val nonExamPoints: String = "",
    val teacherPersona: String = "",
    val myLevel: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/** 消息（append-only：模式切换/追问等都是追加行，绝不 UPDATE 内容——缓存核心约束） */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    /** user / assistant / system_notice */
    val role: String,
    val content: String,
    /** 用户消息附带图片（base64，JPEG） */
    val imageBase64: String? = null,
    val imageMime: String? = null,
    /** 附加文档文件名（内容已提取进 content） */
    val attachmentName: String? = null,
    /** token 用量（assistant 消息；对话详情展示缓存命中率） */
    val usageInput: Int? = null,
    val usageOutput: Int? = null,
    val usageCached: Int? = null,
    val createdAt: Long,
)

/** 划重点（按对话分组时间线） */
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long?,
    /** 划重点时的原文 */
    val sourceText: String,
    /** LLM 提炼的要点（Markdown 列表） */
    val points: String,
    val createdAt: Long,
)

/** 例题本（出例题卡片加入） */
@Entity(tableName = "examples")
data class ExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long?,
    val question: String,
    val solution: String,
    /** 考点标签 */
    val tags: String,
    val createdAt: Long,
)

/** 练题记录（出题→作答→批改→解析，可回看） */
@Entity(tableName = "practice_attempts")
data class PracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 主题，如「第二类换元」 */
    val topic: String,
    /** 基础 / 进阶 / 挑战 */
    val difficulty: String,
    /** LLM 出的题目 */
    val question: String,
    /** 用户作答（文本；拍照作答 OCR 后的文本） */
    val answer: String? = null,
    /** 作答图片（base64） */
    val answerImageBase64: String? = null,
    /** AI 批改点评 + 详细解析（Markdown） */
    val review: String? = null,
    val createdAt: Long,
)

data class ConversationWithMessages(
    @Embedded val conversation: ConversationEntity,
    @Relation(parentColumn = "id", entityColumn = "conversationId")
    val messages: List<MessageEntity>,
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, title: String, now: Long)

    @Query("UPDATE conversations SET mode = :mode, updatedAt = :now WHERE id = :id")
    suspend fun updateMode(id: Long, mode: String, now: Long)

    @Query("UPDATE conversations SET layer2Prompt = :layer2, examFocus = :examFocus, nonExamPoints = :nonExam, teacherPersona = :persona, myLevel = :level, updatedAt = :now WHERE id = :id")
    suspend fun updateConvoSettings(
        id: Long,
        layer2: String,
        examFocus: String,
        nonExam: String,
        persona: String,
        level: String,
        now: Long,
    )
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    fun observeByConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    suspend fun getByConversation(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND role = 'assistant' ORDER BY id DESC LIMIT 1")
    suspend fun lastAssistantMessage(conversationId: Long): MessageEntity?

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)

    @Query("SELECT MAX(id) FROM messages WHERE conversationId = :conversationId AND role = 'assistant'")
    suspend fun lastAssistantMessageId(conversationId: Long): Long?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insert(highlight: HighlightEntity): Long

    @Delete
    suspend fun delete(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ExampleDao {
    @Query("SELECT * FROM examples ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExampleEntity>>

    @Insert
    suspend fun insert(example: ExampleEntity): Long

    @Delete
    suspend fun delete(example: ExampleEntity)

    @Query("DELETE FROM examples WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PracticeDao {
    @Query("SELECT * FROM practice_attempts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PracticeEntity>>

    @Query("SELECT * FROM practice_attempts ORDER BY createdAt DESC")
    suspend fun getAll(): List<PracticeEntity>

    @Query("SELECT * FROM practice_attempts WHERE id = :id")
    suspend fun getById(id: Long): PracticeEntity?

    @Insert
    suspend fun insert(practice: PracticeEntity): Long

    @Update
    suspend fun update(practice: PracticeEntity)

    @Delete
    suspend fun delete(practice: PracticeEntity)

    @Query("DELETE FROM practice_attempts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
