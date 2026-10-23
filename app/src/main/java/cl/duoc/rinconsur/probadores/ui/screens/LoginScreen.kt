package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.LoginViewModel

// Ingreso de colaboradores con RUT validado (RF-10).
@Composable
fun LoginScreen(
    onVolver: () -> Unit,
    onIngreso: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_login), onVolver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioGrande),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                "Ingrese su RUT para registrar acciones en la sección",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(Modifier.widthIn(max = Dimens.anchoMaximoFormulario), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                CampoTexto(
                    valor = estado.rut,
                    onCambio = viewModel::onRutChange,
                    etiqueta = "RUT",
                    icono = Icons.Filled.Person,
                    error = estado.errores.rut,
                    ayuda = "Con dígito verificador, con o sin puntos",
                    placeholder = "12.345.678-9",
                    opcionesTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    accionesTeclado = KeyboardActions(onDone = { viewModel.ingresar(onIngreso) })
                )

                Button(
                    onClick = { viewModel.ingresar(onIngreso) },
                    enabled = !estado.cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBoton)
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.btn_ingresar), style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Datos FICTICIOS del Anexo 9.2, para probar cada perfil durante la evaluación.
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                        Text("RUT de prueba (Anexo 9.2, ficticios)", style = MaterialTheme.typography.titleMedium)
                        Text("12.984.561-9 · Jefatura (P. Morales)")
                        Text("16.842.311-K · Asistente (C. Fuentes)")
                        Text("17.550.982-8 · Vendedora (J. Muñoz)")
                        Text("18.447.093-4 · Inactivo (F. Torres)")
                    }
                }
            }
        }
    }
}
