# D9 — propietario solo al estar colocado

Regla definitiva del propietario: los spawners como objeto retirado/guardado NO tienen dueño
productivo. Solo los bloques colocados conservan responsable durable. No reatribuir todos los
bloques ya colocados ni limpiar sus asignaciones por esta corrección.

production.7: objetos anteriores con owner/farm PDC se leen sin propietario ni granja. Los
nuevos objetos entregados/retirados dejan de escribir esas dos etiquetas; write limpia únicamente
production_owner_v1/production_farm_v1 en el clon del caller, conserva resto de metadata.
No escaneo global de inventarios/cofres ni migración de toda la BD. Objetos viejos pueden conservar
físicamente PDC previo mientras están almacenados, pero es ignorado para propiedad/cupos.
No modificar IDs/procedencia/lore para afirmar autenticidad; APIreciclaje sigue DRAFT.

Colocación nueva: UUIDdelcolocador (D6) y granja física. Añadir unidades a un stack propio:
se aceptan objetos de cualquier origen previo compatible con tipo/mecánica nativa, y las
unidades consumidas entran bajo el responsable/granja del destino. PDC viejos no bloquean
ni importan presupuesto. Stack ajeno o responsable pendiente sigue rechazado. Nunca cambiar
responsable del bloque ajeno mediante apilado. Protecciones/checks/callbacks/venta/consumo
nativos se conservan y solo después del éxito se modifica stack. Unidades no consumidas siguen
sin propietario colocado; sin nueva identidad/auth runtime por inferencia.

Contiene D7cupos por tipo, D8menú español. Stack50/huevo50/config intacta; stock/loot/XP
sin edición directa. Quitar método de compatibilidad owner/farm en el servicio evita gate viejo;
Legacy helper se conserva como evidencia de regla antigua/tests, sin llamador runtime.

Pruebas159/0fallos y shadowJar solo personal. Regresiones específicas: tag de otro dueño/farm
no impide añadir a stack propio; otra persona no puede añadir; no importa farm vieja. Inspección
source confirma solo2keys PDC retiradas, callbacks intactos. Gameplay pendiente: objeto guardado
antes del cambio, retiro nuevo, entrega/give, stack parcial, target ajeno y no pérdida de items.

JARSHA25642428f42c97ee6dc4ba3e6a8203029f6b5687405f5345c874b25a2c859f99d9d,
instalado Custom. BackupHDD smartspawner-custom-stored-stack-20261011T231511Z (.6 previa),
SQLite consistente3099bloques/17720unidades/integrityok, config intacta. Mensaje de bloqueo
actualizado a dueño del bloque/pending; resto de idioma semánticamente igual (GUI mismo contenido).
Servidor observado detenido con exit0 desde23:11:46Z. No lifecycle ejecutado por este chat.
Activación pendiente próximo arranque del propietario; no pedir reload para activar clases Java.

Rollback: restaurar solo JAR .6 y messages.yml respaldado con autorización de lifecycle. Config/BD
actuales permanecen; no restaurar SQLite viejo sobre cambios posteriores. Retomar .6 restauraría
el bloqueo por metadata vieja; no sirve de solución para objetos guardados. Handoffajeno intacto.
