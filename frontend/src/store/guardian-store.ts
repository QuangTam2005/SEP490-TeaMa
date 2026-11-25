import { create } from 'zustand';

interface GuardianSelectionState {
  selectedStudentId: number | null;
  setSelectedStudentId: (studentId: number | null) => void;
}

export const useGuardianStore = create<GuardianSelectionState>((set) => ({
  selectedStudentId: null,
  setSelectedStudentId: (selectedStudentId) => set({ selectedStudentId }),
}));
