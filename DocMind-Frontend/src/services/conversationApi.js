const getConversationsKey = (userId) => `docmind:${userId}:conversations`;
const getMessagesKey = (userId, conversationId) => `docmind:${userId}:messages:${conversationId}`;

export const conversationApi = {
  list: async (userId) => {
    try {
      const raw = localStorage.getItem(getConversationsKey(userId));
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return parsed.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    } catch (err) {
      console.error('Failed to load conversations from localStorage', err);
      return [];
    }
  },

  getMessages: async (userId, conversationId) => {
    try {
      const raw = localStorage.getItem(getMessagesKey(userId, conversationId));
      if (!raw) return [];
      return JSON.parse(raw);
    } catch (err) {
      console.error('Failed to load messages from localStorage', err);
      return [];
    }
  },

  saveConversation: async (userId, conversation) => {
    try {
      const current = await conversationApi.list(userId);
      const existingIdx = current.findIndex((c) => c.id === conversation.id);
      let updated;
      if (existingIdx >= 0) {
        updated = [...current];
        updated[existingIdx] = conversation;
      } else {
        updated = [conversation, ...current];
      }
      localStorage.setItem(getConversationsKey(userId), JSON.stringify(updated));
    } catch (err) {
      console.error('Failed to save conversation to localStorage', err);
    }
  },

  saveMessages: async (userId, conversationId, messages) => {
    try {
      localStorage.setItem(getMessagesKey(userId, conversationId), JSON.stringify(messages));
    } catch (err) {
      console.error('Failed to save messages to localStorage', err);
    }
  },

  remove: async (userId, conversationId) => {
    try {
      const current = await conversationApi.list(userId);
      const updated = current.filter((c) => c.id !== conversationId);
      localStorage.setItem(getConversationsKey(userId), JSON.stringify(updated));
      localStorage.removeItem(getMessagesKey(userId, conversationId));
    } catch (err) {
      console.error('Failed to remove conversation from localStorage', err);
    }
  },

  clearUserData: (userId) => {
    try {
      const prefix = `docmind:${userId}:`;
      const keysToRemove = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(prefix)) {
          keysToRemove.push(key);
        }
      }
      keysToRemove.forEach((k) => localStorage.removeItem(k));
    } catch (err) {
      console.error('Failed to clear user data from localStorage', err);
    }
  },
};

export default conversationApi;