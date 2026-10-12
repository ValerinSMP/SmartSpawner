# D11 — durabilidad personalizada al retirar spawners

SmartSpawner ahora usa Damageable.hasMaxDamage/getMaxDamage para herramientas con límite
personalizado; sin ese componente conserva la durabilidad del material. API disponible desde
Paper 1.21.11: https://jd.papermc.io/paper/1.21.11/org/bukkit/inventory/meta/Damageable.html.
El Pico Dieciochero de Eco tiene max_damage:8124 sobre netherite (normal2031). Antes, desgaste
>=2031 podía eliminarlo al retirar spawners aunque no hubiera agotado el límite8124.
Esto demuestra la incompatibilidad; el log cliente no confirma el desgaste del pico perdido.

Cambio focalizado en SpawnerBreakListener: aplica desgaste sobre el mismo ItemMeta, usa long
para evitar overflow, respeta unbreakable y desgaste desactivado. Conserva pérdida configurada
por unidad retirada (incluidos stacks), sonido de rotura y eliminación cuando alcanza el límite
real. No modifica reglas de Unbreaking/callbacks de daño preexistentes, Mending/Eco, producción,
propiedad, drops, venta, XP, config ni esquema/BD. No recupera herramientas ya desaparecidas.
Incluye D10/admin mine y las entregas anteriores que estaban pendientes en disco.

Build personal limitado: :core:test :core:shadowJar,170pruebas/0fallos/0errores/0omitidas.
9regresiones nuevas: punto2031 de Eco, retiro50 tras desgaste4000, límite8124, límite personalizado
menor, netherite normal al romper y sin romper, irrompible, desgaste0 y overflow.
Pruebas sin servidor sobre la función de daño que usa el listener; no gameplay ni Eco runtime.
El primer intento de harness requería RegistryAccess de Paper; se aisló la aplicación de daño
con metadata proxy sin cambiar el comportamiento ni añadir dependencias de pruebas.

Custom instalado en disco production.9, sin reinicio/reload. Último enable observado runtime .7;
activación de .9 pendiente del reinicio diario/propietario. Sync/Test sin cambios.
JAR: plugins/SmartSpawner-1.6.6.jar (nombre existente).
SHA256:7a66d64965bf16b79670e76a0366a86dcd386f5c7bff4f1d39d52b99ce0aa9f2.
Backup HDD:/mnt/storage/backups/ai/survival-custom/smartspawner-tool-durability-20261012T014152Z.
Incluye JAR previo .8,config y manifest; hashes verificados. DB intacta/no migración.
Rollback: restaurar solo JAR respaldado .8 con permisos originales y lifecycle aprobado,
conservar BD/config actuales. La copia de configuración es evidencia, no requiere restauración.

Tras reinicio: confirmar enable .9; probar primero en Test o con herramienta de prueba Eco
max8124/desgaste2030 y4000, retirar1 y stack50: desgaste+1/+50, herramienta permanece.
Probar cerca8124: rompe solo al agotarse; herramienta vanilla e irrompible conservan reglas.
Revisar drops/venta/XP/Mending y admin /ss mine <jugador> pendiente D10. No usar el pico valioso
del jugador para la primera comprobación. Sin compensaciones, cambios de inventario ni lifecycle.
DEVELOPMENT_HANDOFF.md ajeno intacto/untracked,hash4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.
