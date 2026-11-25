from selenium.common.exceptions import StaleElementReferenceException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


class ProcesoFormPage:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 15)

    def _esperar_carga(self):
        self.wait.until(
            EC.invisibility_of_element_located((By.XPATH, "//*[contains(text(), 'Cargando')]"))
        )

    def _escribir_seguro(self, etiqueta: str, texto: str):
        xpath = (
            f"//label[contains(normalize-space(), '{etiqueta}')]"
            f"/following::*[self::input or self::textarea][not(@disabled) and not(@readonly)][1]"
        )
        for _ in range(4):
            try:
                campo = self.wait.until(EC.visibility_of_element_located((By.XPATH, xpath)))
                self.driver.execute_script(
                    "arguments[0].scrollIntoView({block: 'center'});", campo
                )
                campo.clear()
                campo.send_keys(texto)
                return
            except StaleElementReferenceException:
                continue
        raise StaleElementReferenceException(f"No se pudo escribir en '{etiqueta}'.")

    def crear_proceso(self, nombre: str, descripcion: str, categoria: str):
        btn_nuevo = self.wait.until(
            EC.element_to_be_clickable(
                (By.XPATH, "//*[self::button or self::a][contains(normalize-space(), 'Crear proceso')]")
            )
        )
        btn_nuevo.click()
        self.wait.until(
            EC.visibility_of_element_located(
                (By.XPATH, "//*[self::h1 or self::h2][contains(normalize-space(), 'Crear proceso')]")
            )
        )
        self._escribir_seguro("Nombre", nombre)
        self._escribir_seguro("Descripción", descripcion)
        self._escribir_seguro("Categoría", categoria)

        btn_guardar = self.wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[contains(normalize-space(), 'Guardar')]"))
        )
        btn_guardar.click()

    def esperar_confirmacion_creacion(self, nombre: str) -> str:
        alerta = self.wait.until(
            EC.visibility_of_element_located(
                (By.XPATH, f"//*[contains(normalize-space(), 'Proceso creado:') or contains(normalize-space(), '{nombre}')]")
            )
        )
        return alerta.text

    def editar_nombre_y_guardar(self, nuevo_nombre: str):
        self._esperar_carga()
        # Si estamos en /diagrama, volver a /procesos para usar el botón 'Editar' de la tarjeta
        if "/diagrama" in self.driver.current_url:
            btn_volver = self.wait.until(
                EC.element_to_be_clickable((By.XPATH, "//*[self::a or self::button][contains(normalize-space(), 'Volver a procesos')]"))
            )
            btn_volver.click()
            self._esperar_carga()

        # Hacer clic atómico en el botón 'Editar' de la fila 'Solicitud de compra demo'
        clicked = self.driver.execute_script("""
            const enlaces = Array.from(document.querySelectorAll('a, button'));
            const titulo = enlaces.find(el => (el.innerText || '').includes('Solicitud de compra demo'));
            if (!titulo) return false;
            let contenedor = titulo.parentElement;
            for (let i = 0; i < 4 && contenedor; i++) {
                const btnEditar = Array.from(contenedor.querySelectorAll('button, a'))
                    .find(b => (b.innerText || '').trim() === 'Editar');
                if (btnEditar) {
                    btnEditar.scrollIntoView({block: 'center'});
                    btnEditar.click();
                    return true;
                }
                contenedor = contenedor.parentElement;
            }
            return false;
        """)
        assert clicked, "No se encontró el botón 'Editar' en la tarjeta de 'Solicitud de compra demo'."

        self._esperar_carga()
        self._escribir_seguro("Nombre", nuevo_nombre)

        btn_guardar = self.wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[contains(normalize-space(), 'Guardar')]"))
        )
        self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", btn_guardar)
        btn_guardar.click()
        self._esperar_carga()
