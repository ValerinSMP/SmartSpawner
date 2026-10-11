# D2 — Candidato de observación para Test

Versión 1.8.3-valerin-observe.1, 2026-10-11. Fuente canónica: personal,
rama work/valerin-production-contract-v1. D1 permanece como contrato/API DRAFT.

## Qué se puede probar ahora

Contabilidad de stock **persistido** del servidor local, incluidos chunks descargados:
unidades dentro de stacks, agregado por tipo base y bloques con cantidad inválida.
Sin inferir propietario de interacciones/protecciones; todo el stock carece todavía
de propiedad certificada por esta función. No equivale a capacidad productiva asignada.
No inventa procedencia ni identifica items auténticos del reciclador.

Comando de consola `ss production` o jugador `/ss production`.
Permiso administrativo `smartspawner.command.production`, default op.
No confundir permiso administrativo con cupos de producción.

Configuración:
```yaml
production:
  observation:
    enabled: true
    interval-seconds: 60
```
Requiere reinicio para cambiar estos dos valores; mínimo efectivo 60 segundos.
Consulta SQL agregada única fuera del hilo principal, trabajador único sin consultas
superpuestas, espera entre fin e inicio, timeout de consulta 10 segundos.
Comando lee snapshot inmutable sin JDBC ni solicitudes de refresh.
No escaneos globales por tick, carga de chunks ni lecturas de jugadores async.
Aun así SQL lee filas periódicamente; coste real y TPS pendientes de Test.
Edad es desde lectura SQL, NO desde el último guardado del stock.
Cambios recientes se observan tras autosave normal más intervalo de observación.
No fuerza guardados, ni cambia callbacks o colas. No suma tablas de otros servidores.
Fallo SQL conserva internamente última muestra pero comando informa no disponible.
Cierre cancela trabajador antes de cerrar pool; no ejecuta JDBC adicional sync.

## Prueba de aceptación por el propietario

1. Servidor Test detenido por propietario: respaldar JAR/config/carpeta de datos
   coherente en disco secundario antes del reemplazo. Mantener un único JAR SmartSpawner.
   No copiar BD viva mientras escribe, ni probar sobre datos productivos.
2. Instalar el candidato solo en Test y arrancar. Confirmar versión observe.1
   y ausencia de errores. Esperar primera muestra y ejecutar `ss production`.
3. Colocar stacks de 1, 50 y 100; apilar, separar, romper y usar comandos de tamaño.
   Esperar autosave más observación: comparar unidades conservadas por tipo.
   Probar MOB y ITEM/variantes: aliases del mismo tipo no deben dividir el agregado.
4. Descargar chunk/mundo y repetir: filas persistidas deben seguir contadas.
   Reiniciar Test, comparar stock y loot/XP. Probar huevos: stack >1 sigue bloqueado,
   stack 1 mantiene cambio/cancelación. Probar Mending con Eco y ventas/callbacks.
5. Registrar edad/duración del comando, MSPT/TPS y errores bajo varios activadores.
   Comparar con observación deshabilitada tras reinicio. No se garantiza ausencia
   de lag por pruebas unitarias.
6. No aprobar traslado a Custom todavía: D2 no aplica límites ni API funcional.

## Validación automatizada y límites

107 tests aprobados (5 nuevos SQL/async), cero fallos/errors/skips.
Pruebas con SQLite temporal: stacks/mundos/aliases, invalid legacy sin reparación,
64-bit, snapshot inmutable, SQL fallido, tabla insegura, reapertura durable y
adquisición fuera del hilo llamador/cancelación del trabajador.
No test servidor Paper/Leaf, MySQL real, LuckPerms, Eco, gameplay ni crash de escrow.
No existe escrow ni reserva/consumo aún. JDBC tests solo en personal.

## Migración y rollback

D2 no crea tablas ni modifica filas/esquema. Fuente durable sigue siendo SQL
existente: observador reconstruye snapshot al iniciar. No es un ledger de propiedad.
Rollback: detener Test, conservar respaldo y restaurar el JAR anterior; claves
production.observation pueden quedar ignoradas. No limpiar stock, loot, XP ni BD.
Migraciones upstream existentes siguen su contrato; partir del JAR baseline 1.8.3
ya observado en Custom, aunque su archivo se llame SmartSpawner-1.6.6.jar.
Fuente anterior en commit17c83695; backup previo en
/mnt/storage/backups/ai/survival-custom/smartspawner-production-design-20261011/source-before.tar.gz.

## Artefactos

Plugin SHA-256:
b1d1859f643f51ffd4e2120336b986a342c0b7f3d7d5d15b36fdd9b1b4be1ab5

API DRAFT SHA-256:
f17c078ac5d7046849a9e8766c5422c512eda8c54d5e0a03bbf6a2d31e42c392

No instalado, sin reinicios/recargas ni cambios productivos.
Handoff original sin seguimiento preservado.
Rangos confirmados: 50/60/70/80/90/100 por tipo/responsable.
Respuesta “lo que no leage” expresa prioridad de rendimiento; NO aprueba
100 unidades en ventana 33x33 ni 100 por granja. Cifras siguen pendientes.
Siguientes incrementos: ledger propiedad/migración revisable e identidad/custodia,
proveedor API con reconciliación de caídas, límites personales y espaciales aprobados.
