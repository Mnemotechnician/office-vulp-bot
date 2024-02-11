package io.mnemotechnician.officevulp.extensions

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory.addInfo
import com.kotlindiscord.kord.extensions.annotations.UnexpectedBehaviour
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.components.buttons.EphemeralInteractionButton
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.*
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.followup.PublicFollowupMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.*
import dev.kord.rest.builder.message.create.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.intellij.lang.annotations.Language
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class PanicBunkerInfoExtension : Extension() {
	override val name = "panicbunker"
	val logger = KotlinLogging.logger(name)

	val explanation = """
		FAQ:
		What is a panic bunker? The panic bunker is activated when there's no admins online. Players with less than 24H of playtime on __this__ server are not allowed to connect.

		Why? Because there are certain people who like to raid the server using throwaway accounts in an NRP manner and ruin everyone's day.
		Online admins are required to ensure they get exploded as soon as they do that.

		When will it end? Not anytime soon. But as soon as an admin joins, you will be able to connect. As long as you don't disconnect, you will be able to continue playing after that.
	""".trimIndent()

	@Language("RegExp")
	val patterns = listOf(
		"""question.*panic bunker""",
		"""(what|when).*(is|does).*panic bunker""",
		"""can'?t connect.*panic bunker""",
		"""(why|what'?s).*panic bunker"""
	).map { it.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)) }

	// Assuming that by the time a member hits a week of time on the server, they will either surpass 24h, or learn what the panic bunker means one way or another
	val serverTimeThreshold = 70000.days

	@OptIn(UnexpectedBehaviour::class)
	override suspend fun setup() {
		publicSlashCommand(::PanicBunkerInfoArgs) {
			name = "panic-bunker-info"
			description = "Send the panic bunker info message in the current channel."

			action {
				lateinit var selfMessage: Message
				selfMessage = respond {
					addInfo(user.asUser().username)
					addDeleteButton(user.id) { selfMessage }
				}.message
			}
		}

		// Intercept message create events and, if they satisfy the conditions, respond with the panic bunker info message.
		kord.events
			.filterIsInstance<MessageCreateEvent>()
			.filter { it.message.author != null }
			.filter {
				val joinedAt = it.message.getAuthorAsMemberOrNull()?.joinedAt ?: return@filter false
				val age = Clock.System.now() - joinedAt
				age < serverTimeThreshold
			}.filter {
				patterns.any { pattern -> pattern.containsMatchIn(it.message.content) }
			}.onEach {
				lateinit var selfMessage: Message
				selfMessage = it.message.respond(useReply = true, pingInReply = true) {
					addInfo(it.message.author!!.username)
					addDeleteButton(it.message.author?.id ?: Snowflake.min) { selfMessage }
				}
				logger.info { "Responded to ${it.message.author!!.username}'s message with the panic bunker FAQ" }
			}.launchIn(kord)
	}

	fun MessageBuilder.addInfo(requester: String? = null) {
		embed {
			title = "Panic Bunker"
			description = explanation
			color = Color(0xeb548e)

			if (requester != null) {
				footer {
					text = "Requested by $requester"
				}
			}
		}
	}

	/** Adds a button to delete the message provided by messageGetter, typically the one it's attached to. */
	suspend fun MessageCreateBuilder.addDeleteButton(requesterId: Snowflake? = null, messageGetter: () -> Message) {
		components(1.hours) {
			add(EphemeralInteractionButton<ModalForm>(null).apply {
				label = "Delete"
				style = ButtonStyle.Secondary

				action {
					if (requesterId != null && this@action.user.id != requesterId) {
						respond {
							content = "You can't delete this message!"
						}
						return@action
					}

					messageGetter().delete()

					respond {
						content = "Deleted."
					}
				}
			})
		}
	}

	class PanicBunkerInfoArgs : Arguments() {
	}
}
