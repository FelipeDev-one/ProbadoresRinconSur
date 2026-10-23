package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.FRANJAS
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.ui.components.GraficoFranjas
import cl.duoc.rinconsur.probadores.ui.components.PanelScaffold
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// =====================================================================
//  ResumenScreen — resumen por día y franja para la jefatura (RF-09, HU-09)
//  ---------------------------------------------------------------------
//  Los cálculos los hace ResumenCalculator (model/Resumen.kt); aquí solo
//  se eligen día y franja y se muestran los indicadores y el gráfico.
// =====================================================================

@Composable
fun ResumenScreen(navController: NavController, viewModel: SeccionViewModel) {
    val seccion by viewModel.seccion.collectAsStateWithLifecycle()
    val fechas = remember(seccion) { viewModel.fechasConRegistros() }
    var fecha by rememberSaveable { mutableStateOf(fechas.firstOrNull() ?: Tiempo.fechaIso(System.currentTimeMillis())) }
    var franjaIndice by rememberSaveable { mutableIntStateOf(0) }
    val franja = FRANJAS[franjaIndice]
    val resumen = remember(seccion, fecha, franjaIndice) { viewModel.resumen(fecha, franja) }
    val barras = remember(seccion, fecha) { viewModel.resumenPorFranja(fecha) }
    val ticket = seccion?.parametros?.ticketPromedio ?: 0

    PanelScaffold(
        titulo = stringResource(R.string.titulo_resumen),
        rutaActual = Ruta.Resumen,
        navController = navController,
        viewModel = viewModel
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Dimens.anchoMinimoTarjeta),
            contentPadding = PaddingValues(Dimens.espacioMedio),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    Text("Día", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        fechas.forEach { f ->
                            FilterChip(
                                selected = f == fecha,
                                onClick = { fecha = f },
                                label = { Text(Tiempo.fechaLegible(f)) }
                            )
                        }
                    }
                    Text("Franja horaria", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        FRANJAS.forEachIndexed { i, f ->
                            FilterChip(selected = i == franjaIndice, onClick = { franjaIndice = i }, label = { Text(f.etiqueta) })
                        }
                    }
                }
            }

            val r = resumen
            if (r == null || (r.ingresos == 0 && r.usosCerrados == 0)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Sin registros para este día y franja.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val indicadores = listOf(
                    "Ingresos a la fila" to "${r.ingresos}",
                    "Abandonos" to "${r.abandonos}" + (r.porcentajeAbandono?.let { " (%.0f%%)".format(it) } ?: ""),
                    "Espera promedio" to (r.esperaPromedio?.let { "%.1f min".format(it) } ?: "—"),
                    "Uso promedio" to (r.usoPromedio?.let { "%.1f min".format(it) } ?: "—"),
                    "Conversión del día" to (r.conversionDia?.let { "%.0f%%".format(it * 100) } ?: "—"),
                    "Venta potencial perdida" to pesos(r.ventaPerdida)
                )
                items(indicadores) { (titulo, valor) -> Indicador(titulo, valor) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Venta perdida = ${r.abandonosVentaPerdida} abandono(s) que cuentan × conversión del día × " +
                            "ticket ${pesos(ticket.toDouble())} (RN-09). \"Decidió comprar sin probarse\" y \"Cierre de tienda\" no se cuentan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                GraficoFranjas(barras, Modifier.padding(bottom = Dimens.espacioMedio))
            }
        }
    }
}

// Formato chileno: $27.990
private fun pesos(valor: Double): String = "$" + "%,.0f".format(valor).replace(',', '.')

@Composable
private fun Indicador(titulo: String, valor: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(
            Modifier
                .padding(Dimens.espacioMedio)
                .fillMaxWidth()
        ) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
