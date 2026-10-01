#!/usr/bin/env python3
"""G5.4 — Clés guard.* (guards centralisés §55-56)."""
import io, re, os, json

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

FR = {
    "guard.accessDenied": "Accès refusé",
    "guard.requiredPermission": "Permission requise : ",
    "guard.requiredRoles": "Rôles acceptés : ",
    "guard.noTenant": "Aucune organisation sélectionnée",
    "guard.noTenantDetail": "Choisissez l'organisation dans laquelle vous travaillez.",
    "guard.chooseOrg": "Choisir une organisation",
    "guard.featureUnavailable": "Fonctionnalité non incluse",
    "guard.featureUnavailableDetail": "Le module « {feature} » n'est pas activé sur votre plateforme. Contactez un administrateur.",
    "guard.requestAccess": "Demander l'accès",
    "guard.requestSent": "Demande envoyée aux responsables",
    "guard.requestAlready": "Demande déjà envoyée — en attente de traitement",
    "guard.requestErr": "Échec de l'envoi de la demande",
    "guard.backHome": "Retour à l'accueil",
}
EN = {
    "guard.accessDenied": "Access denied",
    "guard.requiredPermission": "Required permission: ",
    "guard.requiredRoles": "Accepted roles: ",
    "guard.noTenant": "No organization selected",
    "guard.noTenantDetail": "Choose the organization you work in.",
    "guard.chooseOrg": "Choose an organization",
    "guard.featureUnavailable": "Feature not included",
    "guard.featureUnavailableDetail": "The \"{feature}\" module is not enabled on your platform. Contact an administrator.",
    "guard.requestAccess": "Request access",
    "guard.requestSent": "Request sent to the managers",
    "guard.requestAlready": "Request already sent — pending",
    "guard.requestErr": "Failed to send the request",
    "guard.backHome": "Back to home",
}
PT = {
    "guard.accessDenied": "Acesso negado",
    "guard.requiredPermission": "Permissão necessária: ",
    "guard.requiredRoles": "Funções aceites: ",
    "guard.noTenant": "Nenhuma organização selecionada",
    "guard.noTenantDetail": "Escolha a organização onde trabalha.",
    "guard.chooseOrg": "Escolher uma organização",
    "guard.featureUnavailable": "Recurso não incluído",
    "guard.featureUnavailableDetail": "O módulo \"{feature}\" não está ativado na sua plataforma. Contacte um administrador.",
    "guard.requestAccess": "Solicitar acesso",
    "guard.requestSent": "Pedido enviado aos responsáveis",
    "guard.requestAlready": "Pedido já enviado — pendente",
    "guard.requestErr": "Falha ao enviar o pedido",
    "guard.backHome": "Voltar ao início",
}
ES = {
    "guard.accessDenied": "Acceso denegado",
    "guard.requiredPermission": "Permiso requerido: ",
    "guard.requiredRoles": "Roles aceptados: ",
    "guard.noTenant": "Ninguna organización seleccionada",
    "guard.noTenantDetail": "Elija la organización en la que trabaja.",
    "guard.chooseOrg": "Elegir una organización",
    "guard.featureUnavailable": "Función no incluida",
    "guard.featureUnavailableDetail": "El módulo «{feature}» no está activado en su plataforma. Contacte a un administrador.",
    "guard.requestAccess": "Solicitar acceso",
    "guard.requestSent": "Solicitud enviada a los responsables",
    "guard.requestAlready": "Solicitud ya enviada — pendiente",
    "guard.requestErr": "Error al enviar la solicitud",
    "guard.backHome": "Volver al inicio",
}
SW = {
    "guard.accessDenied": "Ufikiaji Umekataliwa",
    "guard.requiredPermission": "Ruhusa inayohitajika: ",
    "guard.requiredRoles": "Chezo zinazokubalika: ",
    "guard.noTenant": "Hakuna shirika lililochaguliwa",
    "guard.noTenantDetail": "Chagua shirika unalofanya kazi.",
    "guard.chooseOrg": "Chagua shirika",
    "guard.featureUnavailable": "Kipengele haijajumuishwa",
    "guard.featureUnavailableDetail": "Moduli ya \"{feature}\" haiwasilishwa kwenye jukwaa lako. Wasiliana na msimamizi.",
    "guard.requestAccess": "Omba ufikiaji",
    "guard.requestSent": "Ombwa limetumwa kwa wasimamizi",
    "guard.requestAlready": "Ombwa limeshatumwa — lanasubiri",
    "guard.requestErr": "Kushindwa kutuma ombwa",
    "guard.backHome": "Rudi mwanzo",
}
AR = {
    "guard.accessDenied": "الوصول مرفوض",
    "guard.requiredPermission": "الإذن المطلوب: ",
    "guard.requiredRoles": "الأدوار المقبولة: ",
    "guard.noTenant": "لم يتم اختيار أي منظمة",
    "guard.noTenantDetail": "اختر المنظمة التي تعمل فيها.",
    "guard.chooseOrg": "اختيار منظمة",
    "guard.featureUnavailable": "ميزة غير مشمولة",
    "guard.featureUnavailableDetail": "وحدة «{feature}» غير مفعّلة على منصتك. اتصل بمسؤول.",
    "guard.requestAccess": "طلب الوصول",
    "guard.requestSent": "تم إرسال الطلب إلى المسؤولين",
    "guard.requestAlready": "تم إرسال الطلب بالفعل — قيد الانتظار",
    "guard.requestErr": "فشل إرسال الطلب",
    "guard.backHome": "العودة إلى الرئيسية",
}

ALL = {"fr": FR, "en": EN, "pt": PT, "es": ES, "sw": SW, "ar": AR}
ANCHOR = "spaceOs.settingsSaveErr"

for loc, kv in ALL.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "'guard.accessDenied'" in content:
        print(f"{loc}: already present, skip"); continue
    m = re.search(r"( *)'%s':\s*(['\"])(?:.*?)\2,\n" % re.escape(ANCHOR), content)
    if not m:
        print(f"{loc}: anchor {ANCHOR} not found!"); continue
    indent = m.group(1)
    block = "".join("%s'%s': %s,\n" % (indent, k, json.dumps(kv[k], ensure_ascii=False)) for k in FR.keys())
    idx = m.end()
    new = content[:idx] + block + content[idx:]
    with io.open(path, "w", encoding="utf-8") as fh:
        fh.write(new)
    print(f"{loc}: inserted {len(FR)} keys")
