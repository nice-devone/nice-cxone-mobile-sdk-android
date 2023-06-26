/*
 * Copyright (c) 2014-2022. Moxie Software, Inc. and/or its affiliates. All Rights Reserved.
 */

package com.nice.cxonechat.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

internal class ProhibitedCallRule(
    config: Config = Config.empty
) : Rule(config) {
    override val issue = Issue("ProhibitedCall",
            Severity.Maintainability,
            "Using prohibited logging call.",
            Debt.FIVE_MINS
    )

    data class ProhibitedCall(
        val pack: Regex,
        val name: Regex,
        val recommendation: String
    ) {
        constructor(pack: String, name: String, recommendation: String)
        : this(Regex(pack), Regex(name), recommendation)
    }

    private val prohibitions = listOf(
        ProhibitedCall("android.util.Log", ".*", "Use Logger instead of android.util.Log"),
        ProhibitedCall("(|System.out)", "println", "use Logger instead of println")
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        expression.getCallNameExpression()?.let { cne ->
            for(prohibition in prohibitions) {
                if(
                    (cne.text?.let(prohibition.name::matchEntire) == null) or
                    (prohibition.pack.matchEntire(cne.getReceiverExpression()?.text ?: "") == null)
                ) {
                    continue
                }

                report(CodeSmell(
                    issue,
                    Entity.from(expression),
                    prohibition.recommendation
                ))
            }
        }
    }
}
