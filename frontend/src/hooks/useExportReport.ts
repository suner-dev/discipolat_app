import { useState } from 'react';
import api from '@/lib/api';
import toast from 'react-hot-toast';

type ExportFormat = 'html' | 'csv';

interface ExportOptions {
  endpoint: string;
  filename: string;
  format?: ExportFormat;
}

export function useExportReport() {
  const [isExporting, setIsExporting] = useState(false);

  const exportReport = async ({ endpoint, filename, format = 'html' }: ExportOptions) => {
    setIsExporting(true);
    try {
      const response = await api.get(endpoint, {
        responseType: 'blob',
      });

      const mimeType = format === 'csv'
        ? 'text/csv'
        : 'text/html';

      const url = window.URL.createObjectURL(response.data);

      if (format === 'html') {
        // Open in new tab for print/PDF preview (user can Ctrl+P → Save as PDF)
        const newWindow = window.open(url, '_blank');
        if (newWindow) {
          window.URL.revokeObjectURL(url);
          toast.success('Rapport ouvert dans un nouvel onglet — utilisez Ctrl+P pour exporter en PDF');
          setIsExporting(false);
          return;
        }
      }

      // Fallback: direct download for CSV or if popup blocked
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Fichier téléchargé');
    } catch (error) {
      toast.error('Erreur lors de l\'export');
    } finally {
      setIsExporting(false);
    }
  };

  return { exportReport, isExporting };
}
