package io.mnemotechnician.officevulp

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.gateway.*
import io.mnemotechnician.officevulp.extensions.PanicBunkerInfoExtension

private val TOKEN = env("TOKEN")

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		chatCommands {
			enabled = false
		}

		extensions {
			add(::PanicBunkerInfoExtension)
		}

		kord {
			@OptIn(PrivilegedIntent::class)
			intents {
				+ Intent.MessageContent
			}
		}
	}

	bot.start()
}
