package com.serenare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serenare.core.ui.BoxBreathingPath
import com.serenare.core.ui.BreathingCircle
import com.serenare.core.ui.CompletionCard
import com.serenare.core.ui.ExpandableKnowledgeCard
import com.serenare.core.ui.GroundingOrb
import com.serenare.core.ui.HapticPressableSurface
import com.serenare.core.ui.ProgressHeader
import com.serenare.core.ui.SafeScaffold
import com.serenare.core.ui.ScreenBody
import com.serenare.core.ui.SensoryButton
import com.serenare.core.ui.SerenareColors
import com.serenare.core.ui.SerenareTheme
import com.serenare.core.ui.ShimmerText
import com.serenare.core.ui.SoundscapeSelector
import com.serenare.domain.model.ModuleType
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SerenareTheme {
                SerenareApp()
            }
        }
    }
}

@Composable
fun SerenareApp(viewModel: com.serenare.feature.modules.SerenareViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home", modifier = Modifier.background(SerenareColors.Background)) {
        composable("home") {
            HomeScreen(
                onOpenModule = { module ->
                    viewModel.start(module)
                    navController.navigate("module/${module.name}")
                },
                onHelp = { navController.navigate("help") },
                onLibrary = { navController.navigate("library") },
                onHistory = { navController.navigate("history") },
                onProfile = { navController.navigate("profile") }
            )
        }
        composable(
            route = "module/{module}",
            arguments = listOf(navArgument("module") { type = NavType.StringType })
        ) { entry ->
            val module = ModuleType.valueOf(entry.arguments?.getString("module") ?: ModuleType.CRISIS_NOW.name)
            LaunchedEffect(module) { viewModel.start(module) }
            ModuleScreen(module, viewModel, onHelp = { navController.navigate("help") }, onBack = { navController.popBackStack() })
        }
        composable("help") { CrisisHelpScreen(onBack = { navController.popBackStack() }) }
        composable("library") { LibraryScreen(viewModel, onHelp = { navController.navigate("help") }, onBack = { navController.popBackStack() }) }
        composable("history") { HistoryScreen(viewModel, onHelp = { navController.navigate("help") }, onBack = { navController.popBackStack() }) }
        composable("profile") { ProfileScreen(onHelp = { navController.navigate("help") }, onBack = { navController.popBackStack() }) }
    }
}

@Composable
fun HomeScreen(
    onOpenModule: (ModuleType) -> Unit,
    onHelp: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit
) {
    SafeScaffold("SERENARE", "Regulacao emocional e sensorial", onHelp) { padding ->
        ScreenBody {
            Spacer(Modifier.height(padding.calculateTopPadding()))
            Text("Escolha o estado mais proximo deste momento.", color = SerenareColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("O aplicativo funciona offline para os recursos essenciais. Chamadas de IA usam fallback local quando indisponiveis.", color = SerenareColors.TextSecondary)
            SensoryButton("Crise agora", { onOpenModule(ModuleType.CRISIS_NOW) }, accent = SerenareColors.Crisis)
            SensoryButton("Relaxar", { onOpenModule(ModuleType.DAILY) }, accent = SerenareColors.Relax)
            SensoryButton("Foco", { onOpenModule(ModuleType.FOCUS) }, accent = SerenareColors.Focus)
            SensoryButton("Dormir", { onOpenModule(ModuleType.SLEEP) }, accent = SerenareColors.Sleep)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onOpenModule(ModuleType.PANIC) }, modifier = Modifier.weight(1f)) { Text("Panico") }
                OutlinedButton(onClick = { onOpenModule(ModuleType.ANGER) }, modifier = Modifier.weight(1f)) { Text("Tensao") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onOpenModule(ModuleType.DECISION) }, modifier = Modifier.weight(1f)) { Text("Decisao") }
                OutlinedButton(onClick = onLibrary, modifier = Modifier.weight(1f)) { Text("Biblioteca") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) { Text("Historico") }
                OutlinedButton(onClick = onProfile, modifier = Modifier.weight(1f)) { Text("Perfil") }
            }
        }
    }
}

@Composable
fun ModuleScreen(
    module: ModuleType,
    viewModel: com.serenare.feature.modules.SerenareViewModel,
    onHelp: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val steps = moduleSteps(module)
    val accent = accentFor(module)
    SafeScaffold(module.label, "Etapa ${state.step + 1} de ${steps.size}", onHelp, onBack) { padding ->
        ScreenBody {
            Spacer(Modifier.height(padding.calculateTopPadding()))
            ProgressHeader(state.step, steps.size, steps[state.step].first, module.estimatedTime)
            if (state.completed) {
                CompletionCard(state.initialIntensity, state.finalIntensity, state.guidance)
                SensoryButton("Voltar ao inicio", onBack, accent = accent)
                return@ScreenBody
            }
            if (state.loading) ShimmerText()
            when (module) {
                ModuleType.CRISIS_NOW -> CrisisFlow(state.step, accent, viewModel, onHelp, steps.size)
                ModuleType.PANIC -> PanicFlow(state.step, accent, viewModel, onHelp, steps.size)
                ModuleType.ANGER -> AngerFlow(state.step, accent, viewModel, steps.size)
                ModuleType.DAILY -> DailyFlow(state.step, accent, viewModel, steps.size)
                ModuleType.DECISION -> DecisionFlow(state.step, accent, viewModel, steps.size)
                ModuleType.FOCUS -> FocusFlow(state.step, accent, viewModel, steps.size)
                ModuleType.SLEEP -> SleepFlow(state.step, accent, viewModel, steps.size)
            }
        }
    }
}

@Composable
fun CrisisFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, onHelp: () -> Unit, total: Int) {
    var intensity by remember { mutableIntStateOf(5) }
    when (step) {
        0 -> {
            Text("Voce nao precisa resolver nada agora.", color = SerenareColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            SensoryButton("Estou em crise", { viewModel.next(total) }, accent = accent)
            OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) { Text("Preciso de ajuda agora") }
        }
        1 -> {
            Text("Marque a intensidade atual de 0 a 10.", color = SerenareColors.TextPrimary)
            IntensitySelector(intensity, { intensity = it; viewModel.setIntensity(it) })
            if (intensity >= 8) Text("Como a intensidade esta alta, a ajuda imediata esta disponivel no topo da tela.", color = SerenareColors.Crisis)
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        2 -> {
            BreathingCircle("3 - 6", accent, 6)
            Text("Inspire por 3 segundos. Expire por 6 segundos. Repita pelo menos quatro ciclos.", color = SerenareColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::breathIn, modifier = Modifier.weight(1f)) { Text("Inspire") }
                OutlinedButton(onClick = viewModel::breathOut, modifier = Modifier.weight(1f)) { Text("Expire") }
            }
            SensoryButton("Quatro ciclos feitos", { viewModel.next(total) }, accent = accent)
        }
        3 -> {
            Text("Inspire fundo. Inspire mais um pouco no topo. Agora solte tudo.", color = SerenareColors.TextPrimary, fontSize = 20.sp)
            SensoryButton("Primeiro suspiro feito", viewModel::breathOut, accent = accent)
            SensoryButton("Segundo suspiro feito", { viewModel.next(total) }, accent = accent)
        }
        4 -> GroundingStepGroup(viewModel, total, accent)
        5 -> VisualDiversion(viewModel, total, accent)
        6 -> SoundscapeStep(viewModel, total, accent)
        7 -> {
            Text("Marque a intensidade final.", color = SerenareColors.TextPrimary)
            IntensitySelector(intensity, { intensity = it; viewModel.setIntensity(it, final = true) })
            SensoryButton("Encerrar sessao", { viewModel.finish() }, accent = accent)
        }
    }
}

@Composable
fun PanicFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, onHelp: () -> Unit, total: Int) {
    var progress by remember { mutableFloatStateOf(0f) }
    when (step) {
        0 -> {
            Text("Voce esta em seguranca imediata?", color = SerenareColors.TextPrimary, fontSize = 22.sp)
            SensoryButton("Sim", { viewModel.next(total) }, accent = accent)
            OutlinedButton(onClick = { viewModel.next(total) }, modifier = Modifier.fillMaxWidth()) { Text("Nao tenho certeza") }
            OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) { Text("Preciso de ajuda agora") }
        }
        1 -> {
            BoxBreathingPath(progress, accent)
            Text("Respire em quatro tempos: inspire, sustente, expire, sustente.", color = SerenareColors.TextSecondary)
            SensoryButton("Avancar canto", { progress = (progress + 0.25f) % 1f; viewModel.breathIn() }, accent = accent)
            SensoryButton("Tres ciclos feitos", { viewModel.next(total) }, accent = accent)
        }
        2 -> {
            listOf("Pressione os pes no chao", "Una as palmas", "Solte os ombros", "Toque na tela").forEach {
                HapticPressableSurface(it, viewModel::groundingPulse, accent)
            }
            SensoryButton("Anchora fisica concluida", { viewModel.next(total) }, accent = accent)
        }
        3 -> {
            listOf("Isso e intenso, mas passa.", "Seu corpo esta ativado, nao quebrado.", "Volte para este minuto.", "Voce esta respirando. Isso e suficiente.").forEach {
                Text(it, color = SerenareColors.TextPrimary, fontSize = 19.sp)
            }
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        4 -> SoundscapeStep(viewModel, total, accent)
        5 -> SensoryButton("Estou um pouco melhor", { viewModel.finish() }, accent = accent)
    }
}

@Composable
fun AngerFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (step) {
        0 -> {
            Text("Pressione e segure a tela por 3 segundos. Solte devagar.", color = SerenareColors.TextPrimary)
            repeat(4) { HapticPressableSurface("Descarga ${it + 1}", viewModel::releaseAnger, accent) }
            SensoryButton("Oito repeticoes concluidas", { viewModel.next(total) }, accent = accent)
        }
        1 -> {
            BreathingCircle("6 - 8", accent, 8)
            Text("Inspire por 6 segundos e expire por 8 segundos.", color = SerenareColors.TextSecondary)
            SensoryButton("Quatro ciclos feitos", { viewModel.next(total) }, accent = accent)
        }
        2 -> {
            Text("O que esta mais presente?", color = SerenareColors.TextPrimary)
            listOf("Frustrado", "Injusticado", "Sobrecarregado", "Ameacado", "Cansado", "Outro").forEach {
                OutlinedButton(onClick = { viewModel.toggleTag(it) }, modifier = Modifier.fillMaxWidth()) { Text(if (it in state.tags) "$it selecionado" else it) }
            }
            SensoryButton("Continuar", { viewModel.generateForCurrentStep(); viewModel.next(total) }, accent = accent)
        }
        3 -> {
            Text("Perguntas de reenquadramento", color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(state.guidance.ifBlank { "Gerando perguntas seguras..." }, color = SerenareColors.TextSecondary)
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        4 -> {
            listOf("Aguardar 10 min", "Escrever antes de responder", "Conversar depois", "Pedir ajuda").forEach {
                SensoryButton(it, { viewModel.next(total) }, accent = accent)
            }
        }
        5 -> {
            Text("Voce reduziu a chance de agir no impulso. Isso exige coragem.", color = SerenareColors.TextPrimary)
            SensoryButton("Encerrar", { viewModel.finish() }, accent = accent)
        }
    }
}

@Composable
fun DailyFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (step) {
        0 -> listOf("Baixa", "Media", "Suficiente").forEach { SensoryButton(it, { viewModel.setDaily(energy = it); viewModel.next(total) }, accent = accent) }
        1 -> {
            val action = when (state.dailyEnergy) {
                "Baixa" -> "Levante. Alongue dedos das maos por 30 segundos."
                "Media" -> "De 5 passos. Respire fundo ao caminhar."
                else -> "2 minutos de mobilidade: ombros, pescoco e tornozelos."
            }
            Text(action, color = SerenareColors.TextPrimary, fontSize = 20.sp)
            SensoryButton("Microacao feita", { viewModel.generateForCurrentStep(); viewModel.next(total) }, accent = accent)
        }
        2 -> {
            Text(state.guidance.ifBlank { "Beba um copo de agua agora." }, color = SerenareColors.TextPrimary)
            SensoryButton("Fiz isso", { viewModel.next(total) }, accent = accent)
            OutlinedButton(onClick = { viewModel.next(total) }, modifier = Modifier.fillMaxWidth()) { Text("Nao agora") }
        }
        3 -> {
            var achievement by remember { mutableStateOf(state.dailyAchievement) }
            OutlinedTextField(value = achievement, onValueChange = { achievement = it; viewModel.setDaily(achievement = it) }, label = { Text("Conquista do dia") }, modifier = Modifier.fillMaxWidth())
            SensoryButton("Salvar conquista", { viewModel.next(total) }, accent = accent, enabled = achievement.isNotBlank())
        }
        4 -> {
            Text("Isso conta. Mesmo que pareca pouco.", color = SerenareColors.TextPrimary)
            SensoryButton("Encerrar", { viewModel.finish() }, accent = accent)
        }
    }
}

@Composable
fun DecisionFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (step) {
        0 -> {
            listOf("Pes no chao", "Peso do corpo", "Soltar ombros", "Observar maos", "Nomear lugar, hora e situacao").forEach {
                HapticPressableSurface(it, viewModel::groundingPulse, accent)
            }
            SensoryButton("Presenca concluida", { viewModel.next(total) }, accent = accent)
        }
        1 -> {
            Text("A decisao precisa ser tomada agora?", color = SerenareColors.TextPrimary)
            listOf("Sim", "Nao", "Nao sei").forEach { SensoryButton(it, { viewModel.next(total) }, accent = accent) }
        }
        2 -> ProsConsEditor(state.pros, state.cons, viewModel, total, accent)
        3 -> {
            Text(state.guidance.ifBlank { "Aguardando sintese." }, color = SerenareColors.TextPrimary)
            SensoryButton("Gerar sintese", viewModel::generateForCurrentStep, accent = accent)
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        4 -> {
            var action by remember { mutableStateOf("") }
            OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("Minha proxima acao prudente sera...") }, modifier = Modifier.fillMaxWidth())
            SensoryButton("Encerrar", { viewModel.finish() }, accent = accent, enabled = action.isNotBlank())
        }
    }
}

@Composable
fun FocusFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (step) {
        0 -> {
            var title by remember { mutableStateOf(state.taskTitle) }
            var reason by remember { mutableStateOf(state.taskReason) }
            OutlinedTextField(title, { title = it; viewModel.setTask(title, reason) }, label = { Text("O que precisa ser feito agora?") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reason, { reason = it; viewModel.setTask(title, reason) }, label = { Text("Por que isso importa?") }, modifier = Modifier.fillMaxWidth())
            SensoryButton("Quebrar em tres passos", { viewModel.generateForCurrentStep(); viewModel.next(total) }, accent = accent, enabled = title.isNotBlank())
        }
        1 -> {
            (state.focusSteps.ifEmpty { listOf("Abra o material necessario", "Complete a primeira parte menor", "Revise o que foi feito") }).forEach {
                Text(it, color = SerenareColors.TextPrimary, fontSize = 18.sp)
            }
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        2 -> {
            BreathingCircle("5 - 5", accent, 5)
            Text("Respiracao de entrada por dois minutos.", color = SerenareColors.TextSecondary)
            SensoryButton("Entrada feita", { viewModel.next(total) }, accent = accent)
        }
        3 -> SoundscapeStep(viewModel, total, accent)
        4 -> {
            Text("Pomodoro", color = SerenareColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Escolha um padrao e mantenha a tarefa visivel.", color = SerenareColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("25/5", "15/5", "50/10").forEach {
                    OutlinedButton(onClick = viewModel::groundingPulse, modifier = Modifier.weight(1f)) { Text(it) }
                }
            }
            var dump by remember { mutableStateOf("") }
            OutlinedTextField(dump, { dump = it }, label = { Text("Despejo mental") }, modifier = Modifier.fillMaxWidth())
            SensoryButton("Salvar distracao", { viewModel.addMentalDump(dump); dump = "" }, accent = accent, enabled = dump.isNotBlank())
            SensoryButton("Finalizar foco", { viewModel.finish() }, accent = accent)
        }
    }
}

@Composable
fun SleepFlow(step: Int, accent: Color, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int) {
    when (step) {
        0 -> {
            listOf("Pes", "Pernas", "Abdomen", "Ombros", "Rosto").forEach {
                HapticPressableSurface("Tensionar e soltar: $it", viewModel::groundingPulse, accent)
            }
            SensoryButton("Relaxamento concluido", { viewModel.next(total) }, accent = accent)
        }
        1 -> {
            BreathingCircle("4 - 7 - 8", accent, 8)
            Text("Inspire 4, sustente 7, expire 8.", color = SerenareColors.TextSecondary)
            SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
        }
        2 -> SoundscapeStep(viewModel, total, accent)
        3 -> {
            var worry by remember { mutableStateOf("") }
            OutlinedTextField(worry, { worry = it }, label = { Text("Anote uma preocupacao para amanha") }, modifier = Modifier.fillMaxWidth())
            Text("Voce anotou. Pode deixar isso aqui por agora.", color = SerenareColors.TextSecondary)
            SensoryButton("Encerrar ritual", { viewModel.finish() }, accent = accent, enabled = worry.isNotBlank())
        }
    }
}

@Composable
fun GroundingStepGroup(viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int, accent: Color) {
    val steps = listOf("5 coisas para ver", "4 sensacoes fisicas", "3 sons", "2 cheiros ou temperaturas", "1 gosto ou sensacao")
    steps.forEach { label -> GroundingOrb(label, viewModel::groundingPulse, accent) }
    SensoryButton("Grounding concluido", { viewModel.next(total) }, accent = accent)
}

@Composable
fun VisualDiversion(viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int, accent: Color) {
    Text("Toque nos tres pontos em ordem. Mantenha a atencao apenas nesta tarefa visual.", color = SerenareColors.TextPrimary)
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        listOf("1", "2", "3").forEach { GroundingOrb(it, viewModel::groundingPulse, accent) }
    }
    SensoryButton("Sequencia concluida", { viewModel.next(total) }, accent = accent)
}

@Composable
fun SoundscapeStep(viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int, accent: Color) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Text("Escolha uma paisagem sonora. Se uma fonte remota falhar, o app usa fallback procedural offline.", color = SerenareColors.TextSecondary)
    SoundscapeSelector(state.selectedSound, viewModel::selectSound)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("1 min", "3 min", "5 min", "10 min").forEach {
            OutlinedButton(onClick = viewModel::groundingPulse, modifier = Modifier.weight(1f)) { Text(it) }
        }
    }
    SensoryButton("Continuar", { viewModel.next(total) }, accent = accent)
}

@Composable
fun IntensitySelector(value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Intensidade: $value", color = SerenareColors.TextPrimary, fontSize = 24.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            (0..10).forEach { n ->
                OutlinedButton(onClick = { onChange(n) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f)) { Text(n.toString()) }
            }
        }
    }
}

@Composable
fun ProsConsEditor(pros: List<String>, cons: List<String>, viewModel: com.serenare.feature.modules.SerenareViewModel, total: Int, accent: Color) {
    var pro by remember { mutableStateOf("") }
    var con by remember { mutableStateOf("") }
    OutlinedTextField(pro, { pro = it }, label = { Text("Pro") }, modifier = Modifier.fillMaxWidth())
    SensoryButton("Adicionar pro", { viewModel.addPro(pro); pro = "" }, accent = accent, enabled = pro.isNotBlank())
    pros.forEach { Text("Pro: $it", color = SerenareColors.TextSecondary) }
    OutlinedTextField(con, { con = it }, label = { Text("Contra") }, modifier = Modifier.fillMaxWidth())
    SensoryButton("Adicionar contra", { viewModel.addCon(con); con = "" }, accent = accent, enabled = con.isNotBlank())
    cons.forEach { Text("Contra: $it", color = SerenareColors.TextSecondary) }
    SensoryButton("Continuar", { viewModel.next(total) }, accent = accent, enabled = pros.isNotEmpty() || cons.isNotEmpty())
}

@Composable
fun CrisisHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun dial(number: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }
    SafeScaffold("Ajuda imediata", "Contatos reais de suporte", onHelp = {}, onBack = onBack) { padding ->
        ScreenBody {
            Spacer(Modifier.height(padding.calculateTopPadding()))
            Text("Se houver risco imediato a vida, ligue para emergencia local.", color = SerenareColors.Crisis, fontWeight = FontWeight.SemiBold)
            Text("APOIO EMOCIONAL E PREVENCAO DO SUICIDIO\nCVV - Centro de Valorizacao da Vida\nTelefone: 188 (gratuito, 24h, sigiloso)\nChat e e-mail: cvv.org.br\n\nEMERGENCIA MEDICA: SAMU 192\nSEGURANCA PUBLICA: Policia Militar 190\nDIREITOS HUMANOS: Disque 100 (gratuito, 24h)\nWhatsApp Disque 100: (61) 99611-0100\n\nCriancas, adolescentes, idosos, PCD, LGBTQIA+, pessoas em situacao de rua ou trabalho analogo a escravidao podem acionar o Disque 100.\n\nSe possivel, entre em contato com uma pessoa de confianca agora.", color = SerenareColors.TextPrimary)
            SensoryButton("Ligar 188 (CVV)", { dial("188") }, accent = SerenareColors.Crisis)
            SensoryButton("Ligar 192 (SAMU)", { dial("192") }, accent = SerenareColors.Crisis)
            SensoryButton("Ligar 190", { dial("190") }, accent = SerenareColors.Crisis)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar ao app") }
        }
    }
}

@Composable
fun LibraryScreen(viewModel: com.serenare.feature.modules.SerenareViewModel, onHelp: () -> Unit, onBack: () -> Unit) {
    val topics = listOf("Ansiedade", "Ataque de panico", "Regulacao da raiva", "Apoio em episodio depressivo", "Decisao sob estresse", "Procrastinacao e foco")
    val library by viewModel.library.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadLibrary(topics) }
    SafeScaffold("Biblioteca", "Conteudo sem markdown literal", onHelp, onBack) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().background(SerenareColors.Background)
        ) {
            items(topics) { topic ->
                ExpandableKnowledgeCard(topic, library[topic] ?: "Carregando conteudo seguro...")
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: com.serenare.feature.modules.SerenareViewModel, onHelp: () -> Unit, onBack: () -> Unit) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val streak by viewModel.dailyStreak.collectAsStateWithLifecycle()
    SafeScaffold("Historico", "Evolucao local e privada", onHelp, onBack) { padding ->
        ScreenBody {
            Spacer(Modifier.height(padding.calculateTopPadding()))
            Text("Streak diario: $streak", color = SerenareColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (sessions.isEmpty()) {
                Text("Nenhuma sessao registrada ainda.", color = SerenareColors.TextSecondary)
            }
            sessions.reversed().forEach { record ->
                Card(colors = CardDefaults.cardColors(containerColor = SerenareColors.Surface), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(record.module.label, color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(DateFormat.getDateTimeInstance().format(Date(record.startedAtEpochMs)), color = SerenareColors.TextSecondary)
                        Text("Intensidade: ${record.initialIntensity ?: "-"} -> ${record.finalIntensity ?: "-"}", color = SerenareColors.TextSecondary)
                        Text("Som: ${record.soundscape?.label ?: "Nao informado"}", color = SerenareColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onHelp: () -> Unit, onBack: () -> Unit) {
    SafeScaffold("Perfil sensorial", "Preferencias locais", onHelp, onBack) { padding ->
        ScreenBody {
            Spacer(Modifier.height(padding.calculateTopPadding()))
            Text("Configure preferencias sensoriais no arquivo local de dados do app quando a build estiver em dispositivo.", color = SerenareColors.TextSecondary)
            Text("Sons preferidos: chuva, oceano, floresta, ruido branco, ruido marrom e drone de foco.", color = SerenareColors.TextPrimary)
            Text("Vibracao: fraca, media, forte ou desativada.", color = SerenareColors.TextPrimary)
            Text("Privacidade: dados de sessao permanecem locais; textos enviados a IA usam fallback local quando a chave Gemini nao esta configurada.", color = SerenareColors.TextPrimary)
        }
    }
}

fun moduleSteps(module: ModuleType): List<Pair<String, String>> = when (module) {
    ModuleType.CRISIS_NOW -> listOf(
        "Entrada segura" to "",
        "Triagem de intensidade" to "",
        "Respiracao 3-6" to "",
        "Suspiro fisiologico" to "",
        "Grounding 5-4-3-2-1" to "",
        "Desvio visual" to "",
        "Paisagem sonora" to "",
        "Encerramento" to ""
    )
    ModuleType.PANIC -> listOf("Seguranca imediata" to "", "Respiracao quadrada" to "", "Ancora fisica" to "", "Reorientacao" to "", "Paisagem de ancoragem" to "", "Encerramento" to "")
    ModuleType.ANGER -> listOf("Descarga fisica" to "", "Respiracao de resfriamento" to "", "Nomeacao" to "", "Reenquadramento" to "", "Escolha de resposta" to "", "Encerramento" to "")
    ModuleType.DAILY -> listOf("Check-in energia" to "", "Microacao fisica" to "", "Autocuidado e conexao" to "", "Conquista do dia" to "", "Encerramento" to "")
    ModuleType.DECISION -> listOf("Presenca corporal" to "", "Urgencia" to "", "Pros e contras" to "", "Sintese" to "", "Compromisso" to "")
    ModuleType.FOCUS -> listOf("Captura" to "", "Tres passos" to "", "Respiracao de entrada" to "", "Ambiente sonoro" to "", "Pomodoro e despejo mental" to "")
    ModuleType.SLEEP -> listOf("Relaxamento muscular" to "", "Respiracao 4-7-8" to "", "Paisagem sonora" to "", "Diario pre-sono" to "")
}

fun accentFor(module: ModuleType): Color = when (module) {
    ModuleType.CRISIS_NOW, ModuleType.PANIC, ModuleType.ANGER -> SerenareColors.Crisis
    ModuleType.FOCUS, ModuleType.DECISION -> SerenareColors.Focus
    ModuleType.SLEEP -> SerenareColors.Sleep
    ModuleType.DAILY -> SerenareColors.Relax
}
