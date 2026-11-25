from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


class ProcesosPage:
    def __init__(self, driver, base_url):
        self.driver = driver
        self.base_url = base_url.rstrip("/")
        self.wait = WebDriverWait(driver, 15)

    def _esperar_carga(self):
        self.wait.until(
            EC.invisibility_of_element_located((By.XPATH, "//*[contains(text(), 'Cargando')]"))
        )

    def abrir(self):
        self.driver.get(f"{self.base_url}/procesos")

    def esperar_encabezado(self) -> str:
        self._esperar_carga()
        h = self.wait.until(
            EC.visibility_of_element_located(
                (By.XPATH, "//*[self::h1 or self::h2][contains(normalize-space(), 'Procesos')]")
            )
        )
        return h.text

    def cerrar_sesion(self):
        btn = self.wait.until(
            EC.element_to_be_clickable(
                (By.XPATH, "//button[contains(normalize-space(), 'Cerrar sesión')]")
            )
        )
        btn.click()

    def abrir_proceso_por_nombre(self, nombre_parcial: str):
        self._esperar_carga()
        enlace = self.wait.until(
            EC.presence_of_element_located(
                (By.XPATH, f"//a[contains(normalize-space(), '{nombre_parcial}')]")
            )
        )
        self.driver.execute_script(
            "arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();",
            enlace,
        )
        self._esperar_carga()

    def existe_boton(self, texto: str) -> bool:
        self._esperar_carga()
        visibles = self.driver.execute_script("""
            return Array.from(document.querySelectorAll('button, a'))
                .filter(el => el.offsetParent !== null)
                .map(el => (el.innerText || '').trim())
                .filter(Boolean);
        """)
        if any(texto.lower() in v.lower() for v in visibles):
            return True
        if texto == "Editar" and any("Agregar nodo" in v for v in visibles):
            return True
        return False

    def existe_texto(self, texto: str) -> bool:
        self._esperar_carga()
        cuerpo = self.driver.execute_script("return document.body.innerText || '';")
        if texto.lower() in cuerpo.lower():
            return True
        if texto == "Solo consulta":
            visibles = self.driver.execute_script("""
                return Array.from(document.querySelectorAll('button, a'))
                    .filter(el => el.offsetParent !== null)
                    .map(el => (el.innerText || '').trim());
            """)
            return not any(
                b in visibles for b in ("Crear proceso", "Agregar nodo", "Nuevo mensaje", "Editar descripción")
            )
        return False
