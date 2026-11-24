/**
 * Document processing status constants.
 * @readonly
 * @enum {string}
 */
export const DocumentStatus = Object.freeze({
  UPLOADING: 'UPLOADING',
  PROCESSING: 'PROCESSING',
  INDEXED: 'INDEXED',
  FAILED: 'FAILED',
});

/**
 * Chat message role / type constants.
 * @readonly
 * @enum {string}
 */
export const MessageType = Object.freeze({
  USER: 'USER',
  ASSISTANT: 'ASSISTANT',
  SYSTEM: 'SYSTEM',
});

/**
 * File upload status constants.
 * @readonly
 * @enum {string}
 */
export const UploadStatus = Object.freeze({
  PENDING: 'pending',
  UPLOADING: 'uploading',
  DONE: 'done',
  ERROR: 'error',
});

/* =========================================================================
 * JSDoc Type Definitions (matching Spring Boot Backend DTOs & Frontend UI)
 * ========================================================================= */

/**
 * @template T
 * @typedef {Object} ApiResponse
 * @property {boolean} success
 * @property {string|null} message
 * @property {T} data
 * @property {string} timestamp
 */

/**
 * @typedef {Object} UserDto
 * @property {string} id
 * @property {string} username
 * @property {string} email
 * @property {string} role
 */

/**
 * @typedef {Object} LoginRequest
 * @property {string} username
 * @property {string} password
 */

/**
 * @typedef {Object} RegisterRequest
 * @property {string} username
 * @property {string} email
 * @property {string} password
 */

/**
 * @typedef {Object} LoginResponse
 * @property {string} accessToken
 * @property {UserDto} user
 */

/**
 * @typedef {Object} DocumentMetadataDto
 * @property {string} id
 * @property {string} filename
 * @property {string} contentType
 * @property {number} fileSize
 * @property {number|null} totalPages
 * @property {number|null} totalChunks
 * @property {'UPLOADING'|'PROCESSING'|'INDEXED'|'FAILED'} status
 * @property {string|null} errorMessage
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * @typedef {Object} DocumentResponseDto
 * @property {string} id
 * @property {string} fileName
 * @property {number} fileSize
 * @property {'UPLOADING'|'PROCESSING'|'INDEXED'|'FAILED'} status
 * @property {number|null} chunksCreated
 * @property {string} message
 */

/**
 * @typedef {Object} CitationDto
 * @property {string} documentId
 * @property {string} fileName
 * @property {number} chunkIndex
 * @property {number|null} pageNumber
 * @property {string} snippet
 * @property {number} similarityScore
 * @property {Record<string, any>} metadata
 */

/**
 * @typedef {Object} ChatRequestDto
 * @property {string} question
 * @property {string} [documentId]
 * @property {number} [topK]
 * @property {number} [minSimilarity]
 * @property {string} [conversationId]
 */

/**
 * @typedef {Object} ChatResponseDto
 * @property {string} answer
 * @property {string} conversationId
 * @property {CitationDto[]} citations
 * @property {number} responseTimeMs
 */

/**
 * @typedef {Object} SearchRequestDto
 * @property {string} query
 * @property {string} [documentId]
 * @property {number} [topK]
 * @property {number} [similaritySearch]
 */

/**
 * @typedef {Object} SearchResultDto
 * @property {string} query
 * @property {number} totalMatches
 * @property {CitationDto[]} matches
 */

/**
 * @typedef {Object} Conversation
 * @property {string} id
 * @property {string} title
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * @typedef {Object} ConversationMessage
 * @property {string} id
 * @property {'USER'|'ASSISTANT'|'SYSTEM'} messageType
 * @property {string} content
 * @property {string} createdAt
 * @property {CitationDto[]} [citations]
 * @property {number} [responseTimeMs]
 * @property {boolean} [isStreaming]
 */

/**
 * @typedef {Object} Message
 * @property {string} id
 * @property {'user'|'assistant'} role
 * @property {string} content
 * @property {CitationDto[]} [citations]
 * @property {number} [responseTimeMs]
 * @property {Date} timestamp
 * @property {boolean} [isStreaming]
 */

/**
 * @typedef {Object} UploadingFile
 * @property {string} id
 * @property {File} file
 * @property {number} progress
 * @property {'pending'|'uploading'|'done'|'error'} status
 * @property {string} [error]
 * @property {DocumentResponseDto} [result]
 */