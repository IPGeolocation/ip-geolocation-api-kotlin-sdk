package io.ipgeolocation.sdk.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class IpGeolocationResponse(
    val ip: String? = null,
    val hostname: String? = null,
    val domain: String? = null,
    val location: Location? = null,
    val countryMetadata: CountryMetadata? = null,
    val currency: Currency? = null,
    val network: Network? = null,
    val asn: Asn? = null,
    val company: Company? = null,
    val timeZone: TimeZoneInfo? = null,
    val security: Security? = null,
    val userAgent: UserAgent? = null,
    val abuse: Abuse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Location(
    val continentCode: String? = null,
    val continentName: String? = null,
    val countryCode2: String? = null,
    val countryCode3: String? = null,
    val countryName: String? = null,
    val countryNameOfficial: String? = null,
    val countryCapital: String? = null,
    val stateProv: String? = null,
    val stateCode: String? = null,
    val district: String? = null,
    val city: String? = null,
    val locality: String? = null,
    val accuracyRadius: String? = null,
    val confidence: String? = null,
    val dmaCode: String? = null,
    val zipcode: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @get:JsonProperty("is_eu")
    @param:JsonProperty("is_eu")
    val isEu: Boolean? = null,
    val countryFlag: String? = null,
    val geonameId: String? = null,
    val countryEmoji: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CountryMetadata(
    val callingCode: String? = null,
    val tld: String? = null,
    val languages: List<String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Currency(
    val code: String? = null,
    val name: String? = null,
    val symbol: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Network(
    val connectionType: String? = null,
    val route: String? = null,
    @get:JsonProperty("is_anycast")
    @param:JsonProperty("is_anycast")
    val isAnycast: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Asn(
    val asNumber: String? = null,
    val organization: String? = null,
    val country: String? = null,
    val type: String? = null,
    val domain: String? = null,
    val dateAllocated: String? = null,
    val rir: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Company(
    val name: String? = null,
    val type: String? = null,
    val domain: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Security(
    val threatScore: Double? = null,
    @get:JsonProperty("is_tor")
    @param:JsonProperty("is_tor")
    val isTor: Boolean? = null,
    @get:JsonProperty("is_proxy")
    @param:JsonProperty("is_proxy")
    val isProxy: Boolean? = null,
    val proxyProviderNames: List<String>? = null,
    val proxyConfidenceScore: Double? = null,
    val proxyLastSeen: String? = null,
    @get:JsonProperty("is_residential_proxy")
    @param:JsonProperty("is_residential_proxy")
    val isResidentialProxy: Boolean? = null,
    @get:JsonProperty("is_vpn")
    @param:JsonProperty("is_vpn")
    val isVpn: Boolean? = null,
    val vpnProviderNames: List<String>? = null,
    val vpnConfidenceScore: Double? = null,
    val vpnLastSeen: String? = null,
    @get:JsonProperty("is_relay")
    @param:JsonProperty("is_relay")
    val isRelay: Boolean? = null,
    val relayProviderName: String? = null,
    @get:JsonProperty("is_anonymous")
    @param:JsonProperty("is_anonymous")
    val isAnonymous: Boolean? = null,
    @get:JsonProperty("is_known_attacker")
    @param:JsonProperty("is_known_attacker")
    val isKnownAttacker: Boolean? = null,
    @get:JsonProperty("is_bot")
    @param:JsonProperty("is_bot")
    val isBot: Boolean? = null,
    @get:JsonProperty("is_spam")
    @param:JsonProperty("is_spam")
    val isSpam: Boolean? = null,
    @get:JsonProperty("is_cloud_provider")
    @param:JsonProperty("is_cloud_provider")
    val isCloudProvider: Boolean? = null,
    val cloudProviderName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Abuse(
    val route: String? = null,
    val country: String? = null,
    val name: String? = null,
    val organization: String? = null,
    val kind: String? = null,
    val address: String? = null,
    val emails: List<String>? = null,
    val phoneNumbers: List<String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeZoneInfo(
    val name: String? = null,
    val offset: Double? = null,
    val offsetWithDst: Double? = null,
    val currentTime: String? = null,
    val currentTimeUnix: Double? = null,
    val currentTzAbbreviation: String? = null,
    val currentTzFullName: String? = null,
    val standardTzAbbreviation: String? = null,
    val standardTzFullName: String? = null,
    @get:JsonProperty("is_dst")
    @param:JsonProperty("is_dst")
    val isDst: Boolean? = null,
    val dstSavings: Double? = null,
    val dstExists: Boolean? = null,
    val dstTzAbbreviation: String? = null,
    val dstTzFullName: String? = null,
    val dstStart: DstTransition? = null,
    val dstEnd: DstTransition? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DstTransition(
    val utcTime: String? = null,
    val duration: String? = null,
    val gap: Boolean? = null,
    val dateTimeAfter: String? = null,
    val dateTimeBefore: String? = null,
    val overlap: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAgent(
    val userAgentString: String? = null,
    val name: String? = null,
    val type: String? = null,
    val version: String? = null,
    val versionMajor: String? = null,
    val device: UserAgentDevice? = null,
    val engine: UserAgentEngine? = null,
    val operatingSystem: UserAgentOperatingSystem? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAgentDevice(
    val name: String? = null,
    val type: String? = null,
    val brand: String? = null,
    val cpu: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAgentEngine(
    val name: String? = null,
    val type: String? = null,
    val version: String? = null,
    val versionMajor: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAgentOperatingSystem(
    val name: String? = null,
    val type: String? = null,
    val version: String? = null,
    val versionMajor: String? = null,
    val build: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BulkLookupError(
    val message: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BulkLookupResult(
    val data: IpGeolocationResponse? = null,
    val error: BulkLookupError? = null,
) {
    @JsonIgnore
    fun isSuccess(): Boolean = data != null && error == null
}
