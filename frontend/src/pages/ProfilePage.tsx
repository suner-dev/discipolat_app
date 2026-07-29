import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import {
  User, Mail, Shield, Calendar, CheckCircle, XCircle, Edit3, Save, X, Lock,
  Eye, EyeOff, Loader2, Phone, Heart, Sparkles, Key, ChevronDown, ChevronUp,
} from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_LABELS: Record<string, string> = {
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable de département',
  FAISEUR: 'Faiseur de disciples',
};

const SITUATION_LABELS: Record<string, string> = {
  CELIBATAIRE: 'Célibataire',
  MARIE: 'Marié(e)',
  DIVORCE: 'Divorcé(e)',
  VEUF: 'Veuf/Veuve',
  AUTRE: 'Autre',
};

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [editData, setEditData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    phone: user?.phone || '',
    dateNaissance: user?.dateNaissance || '',
    situationFamiliale: user?.situationFamiliale || '',
  });

  const [passwordData, setPasswordData] = useState({
    currentPassword: '', newPassword: '', confirmPassword: '',
  });
  const [passwordErrors, setPasswordErrors] = useState<Record<string, string>>({});

  const updateProfileMutation = useMutation({
    mutationFn: async (data: typeof editData) => {
      const res = await api.put('/users/me', {
        ...data,
        dateNaissance: data.dateNaissance || undefined,
        situationFamiliale: data.situationFamiliale || undefined,
      });
      return res.data;
    },
    onSuccess: (data) => {
      updateUser(data);
      queryClient.invalidateQueries({ queryKey: ['user'] });
      toast.success('Profil mis à jour avec succès');
      setIsEditing(false);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const changePasswordMutation = useMutation({
    mutationFn: async (data: { currentPassword: string; newPassword: string }) => {
      await api.post('/auth/change-password', data);
    },
    onSuccess: () => {
      toast.success('Mot de passe changé avec succès');
      setShowPasswordForm(false);
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setPasswordErrors({});
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const validatePassword = (): boolean => {
    const errors: Record<string, string> = {};
    if (!passwordData.currentPassword) errors.currentPassword = 'Requis';
    if (!passwordData.newPassword) errors.newPassword = 'Requis';
    else if (passwordData.newPassword.length < 8) errors.newPassword = 'Min. 8 caractères';
    else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(passwordData.newPassword))
      errors.newPassword = 'Majuscule, minuscule et chiffre requis';
    if (passwordData.newPassword !== passwordData.confirmPassword)
      errors.confirmPassword = 'Ne correspond pas';
    setPasswordErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handlePasswordSubmit = () => {
    if (validatePassword()) {
      changePasswordMutation.mutate({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      });
    }
  };

  if (!user) return null;

  const infoFields = [
    { icon: Mail, label: 'Email', value: user.email, readonly: true },
    { icon: Phone, label: 'Téléphone', value: user.phone || '-', key: 'phone', readonly: false },
    { icon: Calendar, label: 'Date de naissance', value: user.dateNaissance ? new Date(user.dateNaissance).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) : '-', key: 'dateNaissance', readonly: false, type: 'date' as const },
    { icon: Heart, label: 'Situation familiale', value: user.situationFamiliale ? SITUATION_LABELS[user.situationFamiliale] || user.situationFamiliale : '-', key: 'situationFamiliale', readonly: false, type: 'select' as const },
    { icon: Shield, label: 'Rôle', value: ROLE_LABELS[user.role], readonly: true },
  ];

  return (
    <div className="page-container max-w-2xl">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <User className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Mon profil</h1>
          </div>
          <p className="page-subtitle">Gérez vos informations personnelles</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          {!isEditing && (
            <button onClick={() => setIsEditing(true)} className="btn-secondary btn-sm">
              <Edit3 className="w-4 h-4" /> Modifier
            </button>
          )}
          <button
            onClick={() => setShowPasswordForm(!showPasswordForm)}
            className={`btn-ghost btn-sm ${showPasswordForm ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600' : ''}`}
          >
            <Key className="w-4 h-4" />
            {showPasswordForm ? 'Masquer' : 'Mot de passe'}
          </button>
        </div>
      </div>

      {/* Password change (toggle) */}
      {showPasswordForm && (
        <div className="glass-card p-6 mb-6 animate-slide-up border-l-[3px] border-l-primary-500">
          <div className="flex items-center gap-3 mb-5">
            <div className="p-2.5 rounded-xl bg-primary-100 dark:bg-primary-900/30">
              <Lock className="w-5 h-5 text-primary-600 dark:text-primary-400" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Changer le mot de passe</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">Minimum 8 caractères, majuscule, minuscule, chiffre</p>
            </div>
          </div>
          <div className="space-y-4">
            {(['currentPassword', 'newPassword', 'confirmPassword'] as const).map((field) => {
              const showField = field === 'currentPassword' ? showCurrentPassword
                             : field === 'newPassword' ? showNewPassword : showConfirmPassword;
              const toggleShow = field === 'currentPassword' ? setShowCurrentPassword
                              : field === 'newPassword' ? setShowNewPassword : setShowConfirmPassword;
              return (
                <div key={field}>
                  <label className="label">
                    {field === 'currentPassword' ? 'Mot de passe actuel' : field === 'newPassword' ? 'Nouveau mot de passe' : 'Confirmer'}
                  </label>
                  <div className="relative">
                    <input
                      type={showField ? 'text' : 'password'}
                      className={`input pr-10 ${passwordErrors[field] ? 'input-error' : ''}`}
                      value={passwordData[field]}
                      onChange={(e) => setPasswordData({ ...passwordData, [field]: e.target.value })}
                      placeholder={field === 'currentPassword' ? 'Votre mot de passe actuel' : field === 'newPassword' ? 'Nouveau mot de passe' : 'Confirmez'}
                    />
                    <button type="button" onClick={() => toggleShow(!showField)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                      {showField ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {passwordErrors[field] && <p className="text-xs text-red-500 mt-1">{passwordErrors[field]}</p>}
                </div>
              );
            })}
            <div className="flex justify-end gap-3 pt-2">
              <button onClick={() => { setShowPasswordForm(false); setPasswordErrors({}); }} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={handlePasswordSubmit} disabled={changePasswordMutation.isPending} className="btn-primary btn-sm">
                {changePasswordMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Lock className="w-4 h-4" />}
                Changer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Profile card */}
      <div className="glass-card p-6 animate-slide-up">
        {/* Avatar header */}
        <div className="flex items-center gap-5 mb-8 pb-6 border-b border-white/20 dark:border-white/[0.06]">
          <div className="relative">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700 
                          flex items-center justify-center shadow-glow">
              <span className="text-3xl font-bold text-white drop-shadow-sm">
                {user.firstName?.[0]}{user.lastName?.[0]}
              </span>
            </div>
            <span className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-green-500 border-2 border-white dark:border-gray-900 
                          shadow-[0_0_8px_rgba(34,197,94,0.5)]" />
          </div>
          <div className="flex-1">
            {isEditing ? (
              <div className="flex gap-2">
                <input className="input w-36" value={editData.firstName} onChange={(e) => setEditData({ ...editData, firstName: e.target.value })} placeholder="Prénom" />
                <input className="input w-36" value={editData.lastName} onChange={(e) => setEditData({ ...editData, lastName: e.target.value })} placeholder="Nom" />
              </div>
            ) : (
              <>
                <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100 font-display">
                  {user.firstName} {user.lastName}
                </h2>
                <div className="flex items-center gap-2 mt-1">
                  <span className={`badge text-xs ${user.role === 'PASTEUR' ? 'badge-info' : user.role === 'RESPONSABLE' ? 'badge-warning' : 'badge-success'}`}>
                    {ROLE_LABELS[user.role]}
                  </span>
                  {user.estChefDeFamille && (
                    <span className="badge text-xs bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400 border border-gold-200/50">
                      ⭐ Chef de famille
                    </span>
                  )}
                </div>
              </>
            )}
          </div>
        </div>

        {/* Profile fields */}
        <div className="space-y-1">
          {infoFields.map((field) => (
            <div key={field.label}
              className="flex items-center gap-4 py-3.5 px-3 rounded-xl hover:bg-white/30 dark:hover:bg-gray-800/30 transition-colors group">
              <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800/50 group-hover:bg-white/50 dark:group-hover:bg-gray-700/50 transition-colors">
                <field.icon className="w-4 h-4 text-gray-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-gray-400 font-medium">{field.label}</p>
                {isEditing && !field.readonly ? (
                  field.type === 'select' ? (
                    <select className="input mt-1" value={editData.situationFamiliale} onChange={(e) => setEditData({ ...editData, situationFamiliale: e.target.value })}>
                      <option value="">Sélectionner...</option>
                      {Object.entries(SITUATION_LABELS).map(([k, v]) => (<option key={k} value={k}>{v}</option>))}
                    </select>
                  ) : field.type === 'date' ? (
                    <input type="date" className="input mt-1" value={editData.dateNaissance} onChange={(e) => setEditData({ ...editData, dateNaissance: e.target.value })} />
                  ) : (
                    <input className="input mt-1" value={editData[field.key as keyof typeof editData] as string} onChange={(e) => setEditData({ ...editData, [field.key!]: e.target.value })} />
                  )
                ) : (
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{field.value}</p>
                )}
              </div>
              {field.readonly && <span className="text-[10px] text-gray-400 uppercase tracking-wider flex-shrink-0">Lecture</span>}
            </div>
          ))}

          {/* Chef de famille status */}
          <div className="flex items-center gap-4 py-3.5 px-3 rounded-xl hover:bg-white/30 dark:hover:bg-gray-800/30 transition-colors">
            <div className={`p-2 rounded-lg ${user.estChefDeFamille ? 'bg-green-100 dark:bg-green-900/30' : 'bg-gray-100 dark:bg-gray-800/50'}`}>
              {user.estChefDeFamille
                ? <CheckCircle className="w-4 h-4 text-green-600" />
                : <XCircle className="w-4 h-4 text-gray-400" />}
            </div>
            <div className="flex-1">
              <p className="text-xs text-gray-400 font-medium">Chef de famille</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{user.estChefDeFamille ? 'Oui' : 'Non'}</p>
            </div>
          </div>

          {/* Member since */}
          <div className="flex items-center gap-4 py-3.5 px-3 rounded-xl">
            <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800/50">
              <Calendar className="w-4 h-4 text-gray-400" />
            </div>
            <div className="flex-1">
              <p className="text-xs text-gray-400 font-medium">Membre depuis</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                {new Date(user.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
              </p>
            </div>
            <div className="flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 shadow-[0_0_4px_rgba(34,197,94,0.5)]" />
              <span className="text-[10px] text-green-500 font-medium uppercase tracking-wider">Actif</span>
            </div>
          </div>
        </div>

        {/* Edit actions */}
        {isEditing && (
          <div className="flex justify-end gap-3 mt-6 pt-5 border-t border-white/20 dark:border-white/[0.06] animate-slide-up">
            <button onClick={() => { setIsEditing(false); setEditData({ firstName: user?.firstName || '', lastName: user?.lastName || '', phone: user?.phone || '', dateNaissance: user?.dateNaissance || '', situationFamiliale: user?.situationFamiliale || '' }); }} className="btn-secondary btn-sm">
              <X className="w-4 h-4" /> Annuler
            </button>
            <button onClick={() => updateProfileMutation.mutate(editData)} disabled={updateProfileMutation.isPending} className="btn-primary btn-sm">
              {updateProfileMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              Enregistrer
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
