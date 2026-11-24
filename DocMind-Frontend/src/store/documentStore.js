import { create } from 'zustand';
import { documentApi } from '../services/api';
import toast from 'react-hot-toast';

const initialState = {
  documents: [],
  selectedDocumentId: null,
  isLoadingDocuments: false,
  activeTab: 'chat',
  isSidebarOpen: true,
};

export const useDocumentStore = create((set) => ({
  ...initialState,

  setselectedDocumentId: (id) => set({ selectedDocumentId: id }),
  setActiveTab: (tab) => set({ activeTab: tab }),
  setIsSidebarOpen: (v) => set({ isSidebarOpen: v }),

  fetchDocuments: async () => {
    set({ isLoadingDocuments: true });
    try {
      const resp = await documentApi.getMine();
      if (resp.success) {
        set({ documents: resp.data });
      }
    } catch (err) {
      console.error('Failed to fetch user documents', err);
    } finally {
      set({ isLoadingDocuments: false });
    }
  },

  deleteDocument: async (id) => {
    try {
      await documentApi.delete(id);
      set((state) => ({
        documents: state.documents.filter((d) => d.id !== id),
        selectedDocumentId: state.selectedDocumentId === id ? null : state.selectedDocumentId,
      }));
      toast.success('Document deleted successfully');
    } catch {
      toast.error('Failed to delete document');
    }
  },

  reset: () => {
    set({
      ...initialState,
    });
  },
}));

export default useDocumentStore;