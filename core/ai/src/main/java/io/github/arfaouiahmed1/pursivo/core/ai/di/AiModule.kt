package io.github.arfaouiahmed1.pursivo.core.ai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.arfaouiahmed1.pursivo.core.ai.DefaultProviderClientFactory
import io.github.arfaouiahmed1.pursivo.core.ai.DraftingEngine
import io.github.arfaouiahmed1.pursivo.core.ai.GroundedDraftingEngine
import io.github.arfaouiahmed1.pursivo.core.ai.GroundedInsightEngine
import io.github.arfaouiahmed1.pursivo.core.ai.InsightEngine
import io.github.arfaouiahmed1.pursivo.core.ai.ProviderClientFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds abstract fun bindProviderFactory(implementation: DefaultProviderClientFactory): ProviderClientFactory
    @Binds abstract fun bindInsightEngine(implementation: GroundedInsightEngine): InsightEngine
    @Binds abstract fun bindDraftingEngine(implementation: GroundedDraftingEngine): DraftingEngine
}
