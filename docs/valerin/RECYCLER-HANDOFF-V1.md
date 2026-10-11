# Handoff SmartSpawner -> plugin de reciclaje

Entrega D1, 2026-10-11. Contrato: PRODUCTION-OWNERSHIP-API-V1.md.
Interfaz DRAFT compilable: github.nighter.smartspawner.api.production.v1.SpawnerUnitServiceV1.
API MAJOR_VERSION=1. Sin proveedor runtime en este JAR: CANJES NO DISPONIBLES.

El otro proyecto puede preparar adaptador/version negotiation/modelo de operación
contra esta interfaz, sin activar economía ni confiar en lore/PDC aislado.
No usar isSmartSpawner como prueba de procedencia; ISSUED requiere ledger.
No escribir SQLite/MySQL SmartSpawner ni emitir IDs en lore/items/caches.
No añadir tablas propias dentro del almacenamiento SmartSpawner.

Métodos previstos async: inspect(locator), reserve(payload), consume(operation),
release(operation), lookup(operation). El fork es autoridad de unidades/custodia;
el reciclador administra cotización, campaña/presupuesto, menú y shards.
OperationKey incluye autoridad, cliente y UUID. Misma clave+mismo payload debe
replay; payload diferente conflicto. El proveedor autoriza al cliente; conocer IDs
no da derecho a consumo. Un locator/fingerprint/revision vencido se rechaza.
El receipt CONSUMED durable autoriza pagar unidades, no demuestra pago.
RECOVERY_REQUIRED/UNAVAILABLE/LEGACY_UNVERIFIED => detener canje y mostrar motivo.
No reintentar ni devolver automáticamente tras pago de resultado incierto.

No procedencia acreditada del legado: futura campaña necesita admisión explícita
por unidad y presupuesto, nunca titular de protección como comprador automático.
Un cambio por huevo conserva issuedBaseType/provenance; no usar tipo actual para
tasación que permita arbitraje. Stock/propiedad por intervalos; alias/stack/split
no generan unidades nuevas.

Requisitos para integración: proveedor v1, capacidades durable/autenticación/lookup,
pruebas de crash entre reserva, retiro, consumo, pago y confirmación. No simulación
in-memory como evidencia de recuperación durable. APIs de pago externo deben
demostrar deduplicación o permitir conciliación de resultado incierto.

Custom inicialmente. Sync no tiene fork desplegado/autoridad conectada; no clonar
SQLite ni ofrecer canjes allí por inferencia. Ningún lifecycle autorizado en esta entrega.
