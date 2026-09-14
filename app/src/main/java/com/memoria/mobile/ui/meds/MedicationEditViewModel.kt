package com.memoria.mobile.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.mobile.data.ApiResult
import com.memoria.mobile.data.MemoriaRepository
import com.memoria.mobile.data.local.MedicationExtras
import com.memoria.mobile.data.remote.MedicationRequest
import com.memoria.mobile.data.remote.Supplier
import com.memoria.mobile.ui.common.PlanLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MedEditState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "daily",
    val times: List<String> = emptyList(),
    val weekDays: List<Int> = emptyList(),
    val stock: String = "0",
    val instructions: String = "",
    // Fornecedor / Farmácia (Premium) — coluna do servidor, alimenta o alerta de
    // stock crítico por WhatsApp à farmácia.
    val supplierName: String = "",
    val supplierPhone: String = "",
    // Campos que o servidor não guarda: ficam no telefone (LocalStore).
    val treatmentDurationDays: String = "",
    val continuousUse: Boolean = false,
    val prescribingDoctor: String = "",
    val dispensingPharmacy: String = "",
    val isPremium: Boolean = false,
    val saved: Boolean = false,
    /** Set when the free plan is what stopped the save, so the UI can offer Premium. */
    val blockedByPlan: Boolean = false,
)

class MedicationEditViewModel(private val repo: MemoriaRepository) : ViewModel() {

    private val _state = MutableStateFlow(MedEditState())
    val state: StateFlow<MedEditState> = _state.asStateFlow()

    private var editingId: String? = null

    fun start(medicationId: String?) {
        editingId = medicationId
        if (medicationId == null) {
            _state.value = MedEditState(isNew = true)
            viewModelScope.launch { _state.value = _state.value.copy(isPremium = isPremium()) }
            return
        }
        _state.value = _state.value.copy(loading = true, isNew = false)
        viewModelScope.launch {
            val premium = isPremium()
            when (val r = repo.medications()) {
                is ApiResult.Ok -> {
                    val med = r.value.firstOrNull { it.id == medicationId }
                    if (med == null) {
                        _state.value = _state.value.copy(loading = false, error = "Medicamento não encontrado.")
                    } else {
                        val extras = repo.local.medicationExtrasFor(medicationId)
                        _state.value = MedEditState(
                            loading = false,
                            isNew = false,
                            name = med.name,
                            dosage = med.dosage,
                            frequency = med.frequency,
                            times = med.times,
                            weekDays = med.weekDays ?: emptyList(),
                            stock = med.stock.toString(),
                            instructions = med.instructions ?: "",
                            supplierName = med.supplier?.name.orEmpty(),
                            supplierPhone = med.supplier?.phone.orEmpty(),
                            treatmentDurationDays = extras?.treatmentDurationDays?.toString().orEmpty(),
                            continuousUse = extras?.continuousUse ?: false,
                            prescribingDoctor = extras?.prescribingDoctor.orEmpty(),
                            dispensingPharmacy = extras?.dispensingPharmacy.orEmpty(),
                            isPremium = premium,
                        )
                    }
                }
                is ApiResult.Err -> _state.value = _state.value.copy(
                    loading = false,
                    error = r.message,
                    isPremium = premium,
                )
            }
        }
    }

    private suspend fun isPremium(): Boolean =
        (repo.me() as? ApiResult.Ok)?.value?.isPremium == true

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onDosage(v: String) { _state.value = _state.value.copy(dosage = v) }
    fun onStock(v: String) { _state.value = _state.value.copy(stock = v.filter { it.isDigit() }) }
    fun onInstructions(v: String) { _state.value = _state.value.copy(instructions = v) }
    fun onFrequency(v: String) { _state.value = _state.value.copy(frequency = v) }
    fun onSupplierName(v: String) { _state.value = _state.value.copy(supplierName = v) }
    fun onSupplierPhone(v: String) {
        _state.value = _state.value.copy(supplierPhone = v.filter { it.isDigit() }.take(20))
    }
    fun onTreatmentDurationDays(v: String) {
        _state.value = _state.value.copy(treatmentDurationDays = v.filter { it.isDigit() }.take(4))
    }
    fun onContinuousUse(v: Boolean) {
        // Uso contínuo e duração são mutuamente exclusivos: um tratamento sem fim
        // não tem um número de dias, e guardar os dois deixava a ficha a
        // contradizer-se.
        _state.value = _state.value.copy(
            continuousUse = v,
            treatmentDurationDays = if (v) "" else _state.value.treatmentDurationDays,
        )
    }
    fun onPrescribingDoctor(v: String) { _state.value = _state.value.copy(prescribingDoctor = v) }
    fun onDispensingPharmacy(v: String) { _state.value = _state.value.copy(dispensingPharmacy = v) }

    fun addTime(time: String) {
        val t = time.trim()
        if (t.isBlank() || _state.value.times.contains(t)) return
        _state.value = _state.value.copy(times = (_state.value.times + t).sorted())
    }

    fun removeTime(time: String) {
        _state.value = _state.value.copy(times = _state.value.times - time)
    }

    fun toggleWeekDay(day: Int) {
        val current = _state.value.weekDays
        _state.value = _state.value.copy(
            weekDays = if (current.contains(day)) current - day else (current + day).sorted(),
        )
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun save() {
        val s = _state.value
        when {
            s.name.isBlank() -> { _state.value = s.copy(error = "Informe o nome do medicamento."); return }
            s.dosage.isBlank() -> { _state.value = s.copy(error = "Informe a dosagem."); return }
            s.times.isEmpty() -> { _state.value = s.copy(error = "Adicione ao menos um horário."); return }
            s.frequency == "weekly" && s.weekDays.isEmpty() ->
                { _state.value = s.copy(error = "Escolha os dias da semana."); return }
        }
        val request = MedicationRequest(
            name = s.name.trim(),
            dosage = s.dosage.trim(),
            frequency = s.frequency,
            times = s.times,
            weekDays = if (s.frequency == "weekly") s.weekDays else null,
            instructions = s.instructions.trim().ifBlank { null },
            stock = s.stock.toIntOrNull() ?: 0,
            active = true,
            // Only sent by a Premium account. A free one omits the field entirely
            // (null), which the backend reads as "do not touch" — so editing on the
            // phone cannot wipe a pharmacy contact set while the account was paid.
            supplier = if (s.isPremium) {
                Supplier(name = s.supplierName.trim(), phone = s.supplierPhone.trim())
            } else {
                null
            },
        )
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            // The free plan caps active medications, exactly as the website does.
            // The server does NOT enforce this one, so skipping the check here
            // meant the phone handed out for free what the site charges for.
            if (editingId == null && !allowedToAdd()) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = PlanLimits.medicationLimitMessage(),
                    blockedByPlan = true,
                )
                return@launch
            }

            val result = editingId?.let { repo.updateMedication(it, request) }
                ?: repo.createMedication(request)
            when (result) {
                is ApiResult.Ok -> {
                    // Saved after the server call, keyed by the id it assigned: a new
                    // medication has no id to file the extras under until then.
                    result.value.id?.let { id -> saveExtras(id, s) }
                    _state.value = _state.value.copy(saving = false, saved = true)
                }
                is ApiResult.Err -> _state.value = _state.value.copy(saving = false, error = result.message)
            }
        }
    }

    /** The fields the backend has no column for — see [MedicationExtras]. */
    private suspend fun saveExtras(medicationId: String, s: MedEditState) {
        repo.local.saveMedicationExtras(
            MedicationExtras(
                medicationId = medicationId,
                treatmentDurationDays = s.treatmentDurationDays.toIntOrNull()?.takeIf { it > 0 },
                continuousUse = s.continuousUse,
                prescribingDoctor = s.prescribingDoctor.trim(),
                dispensingPharmacy = s.dispensingPharmacy.trim(),
            )
        )
    }

    /**
     * Counts what the SERVER currently holds rather than trusting a screen's
     * cached list — two devices adding at the same time would otherwise both
     * believe there was room.
     */
    private suspend fun allowedToAdd(): Boolean {
        val user = (repo.me() as? ApiResult.Ok)?.value
        val active = (repo.medications() as? ApiResult.Ok)?.value?.count { it.active } ?: 0
        return PlanLimits.canAddMedication(user, active)
    }

    fun clearPlanBlock() { _state.value = _state.value.copy(blockedByPlan = false, error = null) }
}
