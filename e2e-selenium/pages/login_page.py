from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

class LoginPage:
    def __init__(self, driver, base_url):
        self.driver = driver
        self.url = f"{base_url}/login"
        self.wait = WebDriverWait(driver, 10)

    def abrir(self):
        self.driver.get(self.url)

    def iniciar_sesion(self, correo, clave):
        campo_correo = self.wait.until(
            EC.visibility_of_element_located((By.XPATH, "//label[contains(.,'Correo')]/following::input[1]"))
        )
        campo_correo.clear()
        if correo:
            campo_correo.send_keys(correo)

        campo_clave = self.driver.find_element(By.XPATH, "//label[contains(.,'Contraseña')]/following::input[1]")
        campo_clave.clear()
        if clave:
            campo_clave.send_keys(clave)

        self.driver.find_element(By.XPATH, "//button[contains(.,'Ingresar')]").click()

    def obtener_error_visible(self):
        alerta = self.wait.until(
            EC.visibility_of_element_located((By.XPATH, "//*[contains(@class,'alert') or contains(@role,'alert') or contains(.,'inválid') or contains(.,'incorrect')]"))
        )
        return alerta.text