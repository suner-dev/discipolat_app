import toast from 'react-hot-toast';

/**
 * Unified toast notification system.
 * Wraps react-hot-toast with consistent styling and i18n-friendly API.
 */
export const Toast = {
  success(message: string, options?: { duration?: number }) {
    return toast.success(message, {
      duration: options?.duration ?? 3000,
      style: {
        background: '#10B981',
        color: '#fff',
        borderRadius: '12px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
      },
      iconTheme: {
        primary: '#fff',
        secondary: '#10B981',
      },
    });
  },

  error(message: string, options?: { duration?: number }) {
    return toast.error(message, {
      duration: options?.duration ?? 4000,
      style: {
        background: '#EF4444',
        color: '#fff',
        borderRadius: '12px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
      },
      iconTheme: {
        primary: '#fff',
        secondary: '#EF4444',
      },
    });
  },

  warning(message: string, options?: { duration?: number }) {
    return toast(message, {
      duration: options?.duration ?? 3500,
      icon: '⚠️',
      style: {
        background: '#F59E0B',
        color: '#fff',
        borderRadius: '12px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
      },
    });
  },

  info(message: string, options?: { duration?: number }) {
    return toast(message, {
      duration: options?.duration ?? 3000,
      icon: 'ℹ️',
      style: {
        background: '#3B82F6',
        color: '#fff',
        borderRadius: '12px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
      },
    });
  },

  promise<T>(
    promise: Promise<T>,
    msgs: { loading: string; success: string; error: string }
  ) {
    return toast.promise(promise, msgs, {
      style: {
        borderRadius: '12px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
      },
    });
  },

  dismiss(toastId?: string) {
    if (toastId) {
      toast.dismiss(toastId);
    } else {
      toast.dismiss();
    }
  },
};

export default Toast;
