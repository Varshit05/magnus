import { apiClient } from './client';
import type { StudentDTO, DocumentDTO, VerificationResultDTO } from '@/types';

export const studentService = {
  register: async (name: string): Promise<StudentDTO> => {
    const response = await apiClient.post<StudentDTO>('/students/register', { name });
    return response.data;
  },
  listAll: async (): Promise<StudentDTO[]> => {
    const response = await apiClient.get<StudentDTO[]>('/students');
    return response.data;
  },
};

export const documentService = {
  upload: async (file: File, studentId: string): Promise<DocumentDTO> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('studentId', studentId);
    
    const response = await apiClient.post<DocumentDTO>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};

export const verificationService = {
  verifyByFile: async (file: File): Promise<VerificationResultDTO> => {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const response = await apiClient.post<VerificationResultDTO>('/verify', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return error.response.data as VerificationResultDTO;
      }
      throw error;
    }
  },
  getById: async (id: string): Promise<DocumentDTO> => {
    const response = await apiClient.get<DocumentDTO>(`/verify/${id}`);
    return response.data;
  },
  analyzeForensics: async (file: File): Promise<VerificationResultDTO> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<VerificationResultDTO>('/verify/forensic-analysis', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  checkHealth: async (): Promise<string> => {
    const response = await apiClient.get('/health', { responseType: 'text' });
    return response.data;
  },
};
