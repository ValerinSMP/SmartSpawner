# Handoff API tras D3
2026-10-11. Contrato anterior PRODUCTION-OWNERSHIP-API-V1.md permanece DRAFT.
El propietario difirio el proveedor de reciclaje: se venderan spawners, no integrar ahora
reservas/consumo. D3 implementa propiedad/capacidad local, NO la API durable de unidades.

Disponible interno: ownership(blockId), stock(type/quantity/world) y plan/productive.
PDC production_owner_v1 / production_farm_v1 son declaraciones, no certificados.
ID del bloque nuevo es UUID completo; retirar/colocar conserva owner/farm pero genera
otro ID de bloque. No existe identidad ledger individual para splits/merges de items.
Por tanto D3 NO satisface ni anuncia autenticacion, procedencia demostrada o reserva
idempotente por ID de operacion. No usar ItemFactory/SmartSpawnerAPI viejo como certificado.
API events upstream se conservan, incluida venta y cambio de tipo.

Para el futuro proveedor v1: ledger durable de unidades/lotes, emision fuera de templates
cacheados, legacy con procedencia UNPROVEN, identidad conservada en split/merge/retirada,
reserva/consumo/liberacion idempotentes por operationId, reconciliacion inventario/ledger
tras caida y pruebas distribuidas. El consumidor solo usa proveedor versionado; nunca
lore ni escrituras al almacenamiento interno. No reutilizar asignacion de propiedad como
consumo: la auditoria D3 usa UUID de evento nuevo y no registra reserva economica.
No desarrollar ni instalar el reciclador desde este chat.
