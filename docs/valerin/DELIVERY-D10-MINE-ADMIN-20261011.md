# D10 — consulta administrativa de spawners

Uso: /ss mine conserva menú propio; /ss mine <jugador> y /ss mine <jugador> <página>
permiten inspección de otro responsable. Se acepta nombre registrado (conectado o desconectado)
o UUID. /ss mine <página> numérica conserva comportamiento anterior; para nombre puramente
numérico usar UUID para evitar ambigüedad con la página.

Permiso adicional smartspawner.command.mine.others, default op en plugin.yml y paper-plugin.yml.
Sigue exigiendo smartspawner.command.mine (defaulttrue) y permiso del comando raíz. No se asignan
permisos a rangos/usuarios ni se modifica LuckPerms. Sin el adicional no se registra acceso al
argumento de jugador para ese remitente. Revalidado al ejecutar, completar lookup/listado y clicar
menú; revocación impide nuevas consultas/navegación de listas ajenas. El holder distingue UUIDdel
administrador/viewer del responsable/target; filtros y páginas mantienen ese target.

Menú igual a /ss mine: tipos/estado/orden/página/colocados/produciendo, con nombre del responsable
en título/entradas/resumen. Entrada/admin no dice «Tú» para otro usuario. Vista read-only sin
teleport/transferencia/sell/gestión. Bloques sin propietario durable no se atribuyen por última
visita ni titular del terreno para mostrarlos. Objetos en inventarios/cofres no pertenecen al registro.

Lookup de UUID mediante LuckPerms y consulta del listado en worker, sin getOfflinePlayer/red/SQL
main, ni cargar chunks. Reutiliza tokens/inventario esperado para cancelar respuestas antiguas,
close/quit y chequeo de permisos en callback. Consola recibe player_only porque esto abre GUI.
Idioma/claves agregados en todos locales, español en es_ES/DonutSMP/DonutSMP_v2 de Valerin.
Preserva D6–D9: dueño solo colocado, guardados sin dueño, cupos por tipo, stack50/huevo50,
Mending/Eco, datos/loot/XP/callbacks. APIreciclaje sigue DRAFT.

161unitarias/0fallos y shadowJar personal limitado. Nuevos tests de política: consulta propia,
consulta ajena permitida/denegada, revocación, falta permiso base y scope por target/totales.
YAML y nuevas claves admin verificados en todos locales. No gameplay del comando ni GUI probado
en servidor; validar tras activación nombres offline/UUID, filtros, páginas, desconocido/vacío,
usuario normal denegado y quitar permiso con menú abierto. No tests de servidor aparte.

production.8 instalado Custom, sin reinicio/recarga, pendiente próximo arranque/reinicio propietario.
SHA2561659b8035581940b06d4a1e5e809631d731fe599fe30dc66c09d9548a0289d53.
BackupHDD /mnt/storage/backups/ai/survival-custom/smartspawner-custom-mine-admin-20261011T232513Z:
JARprevio.7, config/idiomas y SQLite consistente3098bloques/17695unidades/integrityok.
Config intacta; idiomas cambian solo3claves adminGUI y2avisos nestedmine; semantic diff comprobado.
No lifecycle por este chat. Rollback restaurar JAR.7 y2archivos de idioma respaldados con lifecycle
aprobado por propietario, conservar BDactual/loot/XP y configuración. No restaurar snapshotSQLite.
