import json
import urllib.request
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC


class DiagramaPage:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 15)

    def _esperar_carga(self):
        self.wait.until(
            EC.invisibility_of_element_located((By.XPATH, "//*[contains(text(), 'Cargando')]"))
        )

    def _escribir_input_por_label_en_bloque(self, titulo_bloque: str, etiqueta: str, valor: str):
        ok = self.driver.execute_script("""
            const titulo = Array.from(document.querySelectorAll('h1,h2,h3,h4,strong,div'))
                .find(el => (el.innerText || '').trim() === arguments[0]);
            if (!titulo) return false;
            let box = titulo.parentElement;
            for (let i = 0; i < 4 && box; i++) {
                const labels = Array.from(box.querySelectorAll('label'));
                const lbl = labels.find(l => (l.innerText || '').trim().includes(arguments[1]));
                if (lbl) {
                    const inp = lbl.parentElement.querySelector('input:not([disabled]):not([readonly])')
                        || box.querySelector('input:not([disabled]):not([readonly])');
                    if (inp) {
                        inp.scrollIntoView({block: 'center'});
                        inp.focus();
                        inp.value = arguments[2];
                        inp.dispatchEvent(new Event('input', { bubbles: true }));
                        inp.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    }
                }
                box = box.parentElement;
            }
            return false;
        """, titulo_bloque, etiqueta, valor)
        return bool(ok)

    def agregar_y_editar_actividad(self, nombre_inicial, nombre_editado):
        self._esperar_carga()
        # Abrir el formulario 'Nuevo nodo' si no está desplegado
        self.driver.execute_script("""
            const abierto = Array.from(document.querySelectorAll('*'))
                .some(el => (el.innerText || '').trim() === 'Nuevo nodo');
            if (!abierto) {
                const btn = Array.from(document.querySelectorAll('button'))
                    .find(b => (b.innerText || '').trim() === 'Agregar nodo');
                if (btn) btn.click();
            }
        """)

        self.wait.until(
            EC.visibility_of_element_located((By.XPATH, "//*[normalize-space()='Nuevo nodo']"))
        )

        # Asegurar que la clase seleccionada sea Actividad
        self.driver.execute_script("""
            const titulo = Array.from(document.querySelectorAll('*'))
                .find(el => (el.innerText || '').trim() === 'Nuevo nodo');
            const box = titulo ? titulo.parentElement : document;
            const sel = box.querySelector('select');
            if (sel) {
                const opt = Array.from(sel.options).find(o => o.text.includes('Actividad'));
                if (opt) {
                    sel.value = opt.value;
                    sel.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        """)

        assert self._escribir_input_por_label_en_bloque("Nuevo nodo", "Nombre", nombre_inicial), \
            "No se encontró el input Nombre editable dentro de 'Nuevo nodo'."

        # Clic en el botón Guardar dentro del bloque 'Nuevo nodo'
        guardado = self.driver.execute_script("""
            const titulo = Array.from(document.querySelectorAll('*'))
                .find(el => (el.innerText || '').trim() === 'Nuevo nodo');
            let box = titulo ? titulo.parentElement : null;
            for (let i = 0; i < 4 && box; i++) {
                const btn = Array.from(box.querySelectorAll('button'))
                    .find(b => (b.innerText || '').trim().includes('Guardar') || (b.innerText || '').trim().includes('Agregar'));
                if (btn) {
                    btn.click();
                    return true;
                }
                box = box.parentElement;
            }
            return false;
        """)
        assert guardado, "No se encontró el botón Guardar en 'Nuevo nodo'."
        self._esperar_carga()

        # Verificar que el nodo quedó creado y abrir su edición
        self.wait.until(
            EC.visibility_of_element_located((By.XPATH, f"//*[contains(text(), '{nombre_inicial}')]"))
        )
        self.driver.execute_script("""
            const nodos = Array.from(document.querySelectorAll('*'))
                .filter(el => (el.innerText || '').trim() === arguments[0]);
            if (nodos.length > 0) {
                const nodo = nodos[nodos.length - 1];
                nodo.scrollIntoView({block: 'center'});
                nodo.click();
                let box = nodo.parentElement;
                for (let i = 0; i < 4 && box; i++) {
                    const btnEdit = Array.from(box.querySelectorAll('button'))
                        .find(b => (b.innerText || '').trim() === 'Editar');
                    if (btnEdit) { btnEdit.click(); break; }
                    box = box.parentElement;
                }
            }
        """, nombre_inicial)

        # Escribir el nombre editado en el input editable visible y guardar
        self.driver.execute_script("""
            const inputs = Array.from(document.querySelectorAll('input:not([disabled]):not([readonly])'))
                .filter(i => i.offsetParent !== null && i.value === arguments[0]);
            const inp = inputs[inputs.length - 1]
                || Array.from(document.querySelectorAll('input:not([disabled]):not([readonly])')).filter(i => i.offsetParent !== null).pop();
            if (inp) {
                inp.focus();
                inp.value = arguments[1];
                inp.dispatchEvent(new Event('input', { bubbles: true }));
                inp.dispatchEvent(new Event('change', { bubbles: true }));
            }
            const btnsGuardar = Array.from(document.querySelectorAll('button'))
                .filter(b => b.offsetParent !== null && (b.innerText || '').trim().includes('Guardar'));
            if (btnsGuardar.length > 0) btnsGuardar[btnsGuardar.length - 1].click();
        """, nombre_inicial, nombre_editado)

        self._esperar_carga()
        return self.wait.until(
            EC.visibility_of_element_located((By.XPATH, f"//*[contains(text(), '{nombre_editado}')]"))
        ).is_displayed()

    def compartir_proceso_consulta(self):
        self._esperar_carga()
        self.wait.until(lambda d: "/diagrama" in d.current_url or "/procesos/" in d.current_url)

        proceso_id = self.driver.current_url.split("/procesos/")[1].split("/")[0]
        token = self.driver.execute_script("""
            const buscarEn = (store) => {
                for (let i = 0; i < store.length; i++) {
                    const val = store.getItem(store.key(i));
                    if (!val) continue;
                    if (val.startsWith('eyJ')) return val;
                    try {
                        const obj = JSON.parse(val);
                        if (obj && typeof obj === 'object') {
                            for (const v of Object.values(obj)) {
                                if (typeof v === 'string' && v.startsWith('eyJ')) return v;
                            }
                        }
                    } catch (e) {}
                }
                return null;
            };
            return buscarEn(sessionStorage) || buscarEn(localStorage);
        """)
        assert token, "No se encontró token JWT en sessionStorage ni localStorage."

        req_get = urllib.request.Request(
            f"http://localhost:4200/api/procesos/{proceso_id}",
            headers={"Authorization": f"Bearer {token}"}
        )
        with urllib.request.urlopen(req_get, timeout=10) as resp:
            ficha = json.loads(resp.read().decode("utf-8"))

        payload = json.dumps({
            "empresaId": 2,
            "permitido": True,
            "version": ficha.get("version", 0)
        }).encode("utf-8")

        req_put = urllib.request.Request(
            f"http://localhost:4200/api/procesos/{proceso_id}/comparticion",
            data=payload,
            method="PUT",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            }
        )
        with urllib.request.urlopen(req_put, timeout=10) as resp_put:
            assert resp_put.status == 200
            data = json.loads(resp_put.read().decode("utf-8"))
            assert data.get("activo") is True
            return True
