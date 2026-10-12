export interface StudentDTO {
  id: string;
  name: string;
}

export interface DocumentDTO {
  id: string;
  fileName: string;
  hashValue: string;
  issueDate: string;
  studentId: string;
}

export interface TamperedRegionDTO {
  x: number;
  y: number;
  width: number;
  height: number;
  confidence: number;
  label: string;
}

export interface VerificationResultDTO {
  authentic: boolean;
  status: string;
  documentId?: string;
  message: string;
  mlAnalyzed?: boolean;
  tamperScore?: number;
  tamperVerdict?: string;
  heatmapBase64?: string;
  elaImageBase64?: string;
  tamperedRegions?: TamperedRegionDTO[];
  metrics?: Record<string, any>;
}
