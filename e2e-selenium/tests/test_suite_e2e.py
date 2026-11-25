import time
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from pages.login_page import LoginPage
from pages.procesos_page import ProcesosPage
from pages.proceso_form_page import ProcesoFormPage
from pages.diagrama_page import DiagramaPage
from pages.mensajeria_page import MensajeriaPage

def test_sel_01_contrasena_incorrecta(driver, base_url):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("admin.uno@example.com", "ClaveErronea999!")
    assert login.obtener_error_visible() != ""
    assert "/login" in driver.current_url

def test_sel_02_login_administrador_exitoso(driver, base_url, demo_password):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("admin.uno@example.com", demo_password)
    procesos = ProcesosPage(driver, base_url)
    assert "Procesos" in procesos.esperar_encabezado()
    assert "/procesos" in driver.current_url

def test_sel_03_formulario_incompleto_no_se_envia(driver, base_url):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("", "")
    assert "/login" in driver.current_url

def test_sel_04_cerrar_sesion(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.cerrar_sesion()
    WebDriverWait(driver, 10).until(EC.url_contains("/login"))
    assert "/login" in driver.current_url

def test_sel_05_ruta_protegida_sin_sesion(driver, base_url):
    procesos = ProcesosPage(driver, base_url)
    procesos.abrir()
    WebDriverWait(driver, 10).until(EC.url_contains("/login"))
    assert "/login" in driver.current_url

def test_sel_06_crear_proceso(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    nombre_unico = f"Proceso Selenium {int(time.time())}"
    form = ProcesoFormPage(driver)
    form.crear_proceso(nombre_unico, "Descripción automatizada", "Calidad")
    assert f"Proceso creado: {nombre_unico}" in form.esperar_confirmacion_creacion(nombre_unico)

def test_sel_07_editar_proceso_persiste_tras_recargar(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.abrir_proceso_por_nombre("Solicitud de compra demo")
    nuevo_nombre = f"Solicitud Editada {int(time.time())}"
    form = ProcesoFormPage(driver)
    form.editar_nombre_y_guardar(nuevo_nombre)
    driver.refresh()
    assert procesos.existe_texto(nuevo_nombre)

def test_sel_08_crear_y_editar_actividad_diagrama(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.abrir_proceso_por_nombre("Solicitud")
    diagrama = DiagramaPage(driver)
    assert diagrama.agregar_y_editar_actividad("Actividad Temp", "Actividad Editada Selenium")

def test_sel_09_consultar_mensajes_y_correlacion(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.abrir_proceso_por_nombre("Solicitud")
    mensajeria = MensajeriaPage(driver)
    mensajeria.abrir_seccion_mensajes()
    assert mensajeria.verificar_estructura_visible()

def test_sel_10_permisos_rol_lectura(driver, base_url, demo_password):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("lector.uno@example.com", demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.esperar_encabezado()
    assert not procesos.existe_boton("Crear proceso")
    procesos.abrir_proceso_por_nombre("Solicitud")
    assert procesos.existe_texto("Solo consulta")
    assert not procesos.existe_boton("Agregar nodo")
    assert not procesos.existe_boton("Nuevo mensaje")

def test_sel_11_permisos_rol_editor(driver, base_url, demo_password):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("editor.uno@example.com", demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.esperar_encabezado()
    procesos.abrir_proceso_por_nombre("Solicitud")
    assert procesos.existe_boton("Editar")
    assert not procesos.existe_boton("Configurar")
    assert not procesos.existe_boton("Retirar")

def test_sel_12_aislamiento_multiempresa(driver, base_url, demo_password):
    login = LoginPage(driver, base_url)
    login.abrir()
    login.iniciar_sesion("admin.dos@example.com", demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.esperar_encabezado()
    # Intenta abrir el proceso ID 1 de la empresa 1 por URL directa
    driver.get(f"{base_url}/procesos/1")
    WebDriverWait(driver, 10).until(lambda d: "/procesos/1" not in d.current_url or procesos.existe_texto("denegado") or procesos.existe_texto("no encontrado"))
    assert not procesos.existe_texto("Solicitud de compra demo")

def test_sel_13_compartir_proceso_consulta(driver, base_url, demo_password):
    test_sel_02_login_administrador_exitoso(driver, base_url, demo_password)
    procesos = ProcesosPage(driver, base_url)
    procesos.abrir_proceso_por_nombre("Solicitud")
    diagrama = DiagramaPage(driver)
    diagrama.compartir_proceso_consulta()
    assert "/procesos" in driver.current_url