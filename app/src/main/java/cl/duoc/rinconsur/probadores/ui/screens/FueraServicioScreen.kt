package cl.duoc.rinconsur.probadores.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.permisos.Permisos
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.FotoEvidencia
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.FueraServicioViewModel
import kotlinx.coroutines.launch

// =====================================================================
//  FueraServicioScreen — marcar un probador fuera de servicio (HU-08)
//  con motivo y foto de evidencia tomada con la CÁMARA o elegida de la GALERÍA.
//  ---------------------------------------------------------------------
//  Cámara (4 pasos, ver permisos/Permisos.kt):
//    1) DECLARAR  CAMERA en el Manifest
//    2) VERIFICAR Permisos.tiene(...)
//    3) PEDIR     pedirPermisoCamara.launch(...)  -> diálogo del sistema
//    4) REACCIONAR concedido: abrir cámara · rechazado: Snackbar con "Ajustes"
//
//  La foto se toma con TakePicture: la app de cámara guarda la imagen
//  COMPLETA en el archivo que le indicamos (Uri del FileProvider).
//  Galería: PickVisualMedia (selector de fotos del sistema), sin permisos.
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FueraServicioScreen(
    codigo: String,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: FueraServicioViewModel = viewModel()
) {
    LaunchedEffect(codigo) { viewModel.iniciar(codigo) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // stringResource solo funciona dentro de un @Composable: los textos se preparan aquí.
    val textoPermisoDenegado = stringResource(R.string.permiso_camara_denegado)
    val textoAjustes = stringResource(R.string.btn_ajustes)
    val textoSinCamara = stringResource(R.string.sin_app_camara)
    val textoSinGaleria = stringResource(R.string.sin_app_galeria)

    // PASO 4 (rechazo): avisar y ofrecer abrir Ajustes. Tras 2 rechazos Android ya no
    // vuelve a mostrar el diálogo, así que Ajustes es la única forma de conceder el permiso.
    fun avisarPermisoDenegado() {
        scope.launch {
            val resultado = snackbarHostState.showSnackbar(
                message = textoPermisoDenegado,
                actionLabel = textoAjustes,
                duration = SnackbarDuration.Long
            )
            if (resultado == SnackbarResult.ActionPerformed) Permisos.abrirAjustes(contexto)
        }
    }

    // ------------------------------- CÁMARA -------------------------------

    // "Launcher" = lanzador de otra app que nos devuelve un resultado.
    // TakePicture recibe el Uri donde guardar y responde true si la foto se tomó.
    val lanzadorCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        viewModel.onFotoTomada(exito)
    }

    // Se llama SOLO con el permiso ya concedido.
    fun abrirCamara() {
        try {
            lanzadorCamara.launch(viewModel.prepararFoto())
        } catch (e: ActivityNotFoundException) {
            viewModel.onCamaraNoDisponible(textoSinCamara)   // equipo sin app de cámara: la app no se cae
        }
    }

    // PASO 3: pedir el permiso. El resultado (true / false) llega aquí: PASO 4.
    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else avisarPermisoDenegado()
    }

    // ------------------------------- GALERÍA ------------------------------

    // PickVisualMedia abre el selector de fotos de Android: entrega solo la imagen
    // elegida (Uri) o null si el usuario cerró sin elegir. No requiere permisos.
    val lanzadorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onImagenGaleria(uri)
    }

    fun abrirGaleria() {
        try {
            lanzadorGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            viewModel.mostrarErrorFoto(textoSinGaleria)
        }
    }

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_fuera_servicio, codigo), onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = Dimens.anchoMaximoFormulario), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                Text(
                    "El probador no podrá asignarse y el tiempo estimado se recalculará sin él.",
                    style = MaterialTheme.typography.bodyLarge
                )

                // ------------------------------ MOTIVO ------------------------------
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)
                ) {
                    viewModel.motivosRapidos.forEach { m ->
                        AssistChip(onClick = { viewModel.onMotivoChange(m) }, label = { Text(m) })
                    }
                }
                CampoTexto(
                    valor = estado.motivo,
                    onCambio = viewModel::onMotivoChange,
                    etiqueta = "Motivo",
                    icono = Icons.Filled.Build,
                    error = estado.errores.motivo,
                    ayuda = "${estado.motivo.length}/120",
                    unaLinea = false,
                    lineasMinimas = 2
                )

                // ---------------------------- FOTO ----------------------------
                Text(stringResource(R.string.foto_titulo), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FotoEvidencia(
                        ruta = estado.fotoRuta,
                        descripcion = stringResource(R.string.cd_foto_evidencia, codigo)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            // PASO 2: ¿ya tengo permiso? -> abro la cámara. ¿No? -> lo pido (PASO 3).
                            if (Permisos.tiene(contexto, Permisos.CAMARA)) abrirCamara()
                            else pedirPermisoCamara.launch(Permisos.CAMARA)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.alturaBotonChico)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.btn_camara))
                    }
                    FilledTonalButton(
                        onClick = { abrirGaleria() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.alturaBotonChico)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.btn_galeria))
                    }
                }
                if (estado.fotoRuta != null) {
                    TextButton(onClick = viewModel::quitarFoto, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.btn_quitar_foto))
                    }
                }
                MensajeError(estado.errores.foto)
                MensajeError(estado.errores.general)

                Button(
                    onClick = { viewModel.guardar(onGuardado) },
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBoton)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Marcar fuera de servicio", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
