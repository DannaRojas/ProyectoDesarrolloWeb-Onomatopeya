# Consulta de usuarios por empresa

Primera parte de la integración de Usuario. Solo agrega consultas internas; no crea usuarios, cambia sus datos ni publica rutas HTTP.

## Archivos

- `dto/response/UsuarioResponseDto.java`: id, empresaId, nombre, correo, rolAcceso, estado, fechaCreacion y version. No contiene contraseña, hash, token de invitación ni una entidad JPA anidada.
- `repository/UsuarioConsultaRepository.java`: consultas con `@Query` y JPQL sobre Usuario. Se deja separado para conservar intacto el repositorio de Laura.
- `service/UsuarioConsultaService.java`: valida identificadores, comprueba que exista la empresa y devuelve DTOs.
- `config/ModelMapperConfig.java`: se agregó el mapeo de empresa.id a empresaId. Los demás datos de respuesta se convierten con el mismo ModelMapper que ya utiliza la integración.
- `service/UsuarioConsultaServiceTest.java`, dentro de test: pruebas con H2 temporal.

## Operaciones disponibles en Service

`listarPorEmpresa(empresaId)` devuelve todos los usuarios de esa empresa, ordenados por nombre y después por ID. Incluye INVITADO, ACTIVO e INACTIVO; no decide todavía qué estados puede ver cada rol.

`consultar(empresaId, usuarioId)` busca ambos IDs en una sola consulta JPQL. Si el usuario pertenece a otra empresa, devuelve el mismo error de no encontrado que para un ID inexistente.

Una empresa existente sin usuarios devuelve lista vacía. Una empresa inexistente produce EntityNotFoundException. Los IDs nulos, cero o negativos se rechazan mediante Bean Validation antes de consultar.

La transacción de consulta es de solo lectura. Las consultas no cambian registros ni versiones. El repositorio extiende JpaRepository siguiendo la estructura actual, pero este servicio solo usa los dos métodos JPQL de consulta.

## Límite importante: aún no es autorización

Recibir empresaId permite acotar la consulta, pero no demuestra que el usuario conectado tenga acceso a esa empresa. Antes de agregar un controlador hay que obtener la identidad desde una sesión real, comprobar sus permisos sobre la empresa y definir qué información puede ver cada rol. No aceptar un usuarioActualId del navegador como prueba de identidad.

Este avance no implementa login ni protege por sí mismo la información frente a un llamador que pueda escoger cualquier empresaId. Por eso no se añadió un endpoint ni una pantalla de usuarios. Tampoco se añadieron permisos nuevos o cambios en los roles existentes.

## Qué se conserva

Usuario.java, UsuarioRepository.java y EmpresaService.java siguen como estaban. El alta del administrador inicial continúa a cargo del flujo de Empresa; no se duplica en este servicio.

No se aplicó la migración ni se conectaron estas pruebas a PostgreSQL de Laura. Las comprobaciones en H2 no sustituyen las pruebas pendientes en PostgreSQL.

## Pruebas

Ejecutar desde la raíz:

```powershell
.\mvnw.cmd '-Dtest=UsuarioConsultaServiceTest' test
```

Las siete pruebas revisan filtro por empresa, orden estable, todos los estados, empresa vacía o inexistente, mapeo del DTO sin secretos, usuario ajeno/inexistente, IDs inválidos y conservación de registros/versiones.

Verificación del 27/09/2026: `mvnw.cmd -o -q verify` terminó correctamente con 57 pruebas, cero fallos y cero errores. Incluye las siete pruebas nuevas. Se generó el JAR; la validación sigue siendo local, con H2.

Para comprobar también los módulos previos:

```powershell
.\mvnw.cmd verify
```

## Siguiente parte

Acordar e integrar el control de acceso antes de conectar el controller y sus vistas sencillas. Invitaciones, aceptación, cambios de rol/estado y traslado de empresa siguen pendientes y se implementarán por separado.
