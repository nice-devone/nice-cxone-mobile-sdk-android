package com.nice.cxonechat.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

internal class CXoneChatRuleProvider: RuleSetProvider {
    override val ruleSetId = "cxone-rules"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            ProhibitedCallRule(config)
        )
    )
}
