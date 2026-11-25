from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


class MensajeriaPage:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 15)

    def abrir_seccion_mensajes(self):
        self.wait.until(
            EC.invisibility_of_element_located((By.XPATH, "//*[contains(text(), 'Cargando')]"))
        )
        self.driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        seccion = self.wait.until(
            EC.visibility_of_element_located(
                (By.XPATH, "//button[contains(normalize-space(), 'Nuevo mensaje') or contains(normalize-space(), 'Guardar mensaje') or contains(normalize-space(), 'Nuevo flujo')]")
            )
        )
        self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", seccion)
        return seccion
