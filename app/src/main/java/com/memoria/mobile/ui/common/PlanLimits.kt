package com.memoria.mobile.ui.common

import com.memoria.mobile.data.remote.User

/**
 * What the free plan allows, mirroring `getPlanLimits()` in the web app.
 *
 * The backend enforces the caregiver cap but NOT the medication one, so without
 * this the phone quietly handed out unlimited medications and prescription
 * photos that the website charges for.
 */
object PlanLimits {

    /** Contas gratuitas (após o período de teste de 15 dias) guardam 2 medicamentos; Premium/Trial é ilimitado. */
    const val FREE_MEDICATIONS = 2

    fun hasFullAccess(user: User?): Boolean = user?.isPremium == true

    fun maxMedications(user: User?): Int =
        if (hasFullAccess(user)) Int.MAX_VALUE else FREE_MEDICATIONS

    fun canAddMedication(user: User?, activeCount: Int): Boolean =
        activeCount < maxMedications(user)

    /** Fotos de receitas liberadas durante o período de 15 dias ou com assinatura paga ativa. */
    fun canStorePrescriptions(user: User?): Boolean = hasFullAccess(user)

    fun medicationLimitMessage(user: User? = null): String =
        if (user?.trialExpired == true) {
            "Seu período de teste de 15 dias terminou. O plano gratuito permite $FREE_MEDICATIONS medicamentos. " +
                "Assine para continuar com acesso total a tudo."
        } else {
            "O plano gratuito guarda $FREE_MEDICATIONS medicamentos. " +
                "Ative o Premium para cadastrar quantos precisar."
        }

    fun prescriptionLimitMessage(user: User? = null): String =
        if (user?.trialExpired == true) {
            "Seu período de teste de 15 dias terminou. Assine o plano para continuar com acesso total a fotos de receitas."
        } else {
            "Guardar fotos de receitas é um recurso liberado no período de 15 dias e no plano pago."
        }
}
