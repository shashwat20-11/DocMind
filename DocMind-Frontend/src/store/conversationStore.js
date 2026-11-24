import { create } from 'zustand';
import { conversationApi } from '../services/conversationApi';
import { useAuthStore } from './authStore';

const getUserId = () => useAuthStore.getState().user?.id || 'anonymous';
const getPrefKey = (userId) => `docmind:${userId}:streamingMode`;

const initialConversationId = crypto.randomUUID();

export const useConversationStore = create((set, get) => ({
  conversations: [],
  activeConversationId: initialConversationId,
  messages: {},
  streamingMode: true,
  isLoadingChats: false,

  setStreamingMode: (mode) => {
    const userId = getUserId();
    localStorage.setItem(getPrefKey(userId), JSON.stringify(mode));
    set({ streamingMode: mode });
  },

  loadChats: async () => {
    const userId = getUserId();
    if (userId === 'anonymous') return;

    set({ isLoadingChats: true });
    try {
      // Load streaming preference
      const savedMode = localStorage.getItem(getPrefKey(userId));
      if (savedMode !== null) {
        set({ streamingMode: JSON.parse(savedMode) });
      }

      const chats = await conversationApi.list(userId);
      set({ conversations: chats });

      const currentActiveId = get().activeConversationId;
      if (chats.length > 0 && (!currentActiveId || currentActiveId === initialConversationId)) {
        const firstChat = chats[0];
        set({ activeConversationId: firstChat.id });
        const msgs = await conversationApi.getMessages(userId, firstChat.id);
        set((state) => ({
          messages: { ...state.messages, [firstChat.id]: msgs },
        }));
      }
    } catch (err) {
      console.error('Failed to load chats', err);
    } finally {
      set({ isLoadingChats: false });
    }
  },

  selectChat: async (id) => {
    const userId = getUserId();
    set({ activeConversationId: id });
    const existingMsgs = get().messages[id];
    if (!existingMsgs && userId !== 'anonymous') {
      const msgs = await conversationApi.getMessages(userId, id);
      set((state) => ({
        messages: { ...state.messages, [id]: msgs },
      }));
    }
  },

  newChat: () => {
    const newId = crypto.randomUUID();
    set((state) => ({
      activeConversationId: newId,
      messages: { ...state.messages, [newId]: [] },
    }));
  },

  deleteChat: async (id) => {
    const userId = getUserId();
    if (userId !== 'anonymous') {
      await conversationApi.remove(userId, id);
    }

    set((state) => {
      const nextConversations = state.conversations.filter((c) => c.id !== id);
      const nextMessages = { ...state.messages };
      delete nextMessages[id];

      let nextActiveId = state.activeConversationId;
      if (state.activeConversationId === id) {
        if (nextConversations.length > 0) {
          nextActiveId = nextConversations[0].id;
        } else {
          nextActiveId = crypto.randomUUID();
          nextMessages[nextActiveId] = [];
        }
      }

      return {
        conversations: nextConversations,
        messages: nextMessages,
        activeConversationId: nextActiveId,
      };
    });
  },

  renameChat: async (id, newTitle) => {
    const trimmed = newTitle.trim();
    if (!trimmed) return;
    const userId = getUserId();
    const chat = get().conversations.find((c) => c.id === id);
    if (!chat) return;

    const updatedChat = { ...chat, title: trimmed, updatedAt: new Date().toISOString() };
    set((state) => ({
      conversations: state.conversations.map((c) => (c.id === id ? updatedChat : c)),
    }));

    if (userId !== 'anonymous') {
      await conversationApi.saveConversation(userId, updatedChat);
    }
  },

  appendMessage: async (conversationId, message, customTitle) => {
    const userId = getUserId();
    const currentMsgs = get().messages[conversationId] || [];
    const updatedMsgs = [...currentMsgs, message];

    const now = new Date().toISOString();
    const existingChat = get().conversations.find((c) => c.id === conversationId);
    let updatedChat;

    if (!existingChat) {
      const rawTitle = customTitle || message.content || 'New Chat';
      const title = rawTitle.slice(0, 45) + (rawTitle.length > 45 ? '…' : '');
      updatedChat = {
        id: conversationId,
        title: title.trim() || 'New Chat',
        createdAt: now,
        updatedAt: now,
      };
    } else {
      updatedChat = {
        ...existingChat,
        updatedAt: now,
      };
    }

    set((state) => {
      const otherChats = state.conversations.filter((c) => c.id !== conversationId);
      return {
        conversations: [updatedChat, ...otherChats],
        messages: {
          ...state.messages,
          [conversationId]: updatedMsgs,
        },
      };
    });

    if (userId !== 'anonymous') {
      await conversationApi.saveConversation(userId, updatedChat);
      await conversationApi.saveMessages(userId, conversationId, updatedMsgs);
    }
  },

  updateLastMessageContent: (conversationId, messageId, content, isStreaming = true) => {
    set((state) => {
      const chatMsgs = state.messages[conversationId] || [];
      const updated = chatMsgs.map((m) => (m.id === messageId ? { ...m, content, isStreaming } : m));
      return {
        messages: {
          ...state.messages,
          [conversationId]: updated,
        },
      };
    });
  },

  updateMessageMetadata: async (conversationId, messageId, updates) => {
    const userId = getUserId();
    let updatedList = [];

    set((state) => {
      const chatMsgs = state.messages[conversationId] || [];
      updatedList = chatMsgs.map((m) => (m.id === messageId ? { ...m, ...updates } : m));
      return {
        messages: {
          ...state.messages,
          [conversationId]: updatedList,
        },
      };
    });

    if (userId !== 'anonymous') {
      await conversationApi.saveMessages(userId, conversationId, updatedList);
    }
  },

  getActiveMessages: () => {
    const activeId = get().activeConversationId;
    return get().messages[activeId] || [];
  },

  reset: () => {
    const newId = crypto.randomUUID();
    set({
      conversations: [],
      activeConversationId: newId,
      messages: { [newId]: [] },
      isLoadingChats: false,
    });
  },
}));

export default useConversationStore;