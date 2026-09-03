package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.dataPreview.model.SmxRelationTarget

class SmxRelationResolver(
    private val aliasResolver: SmxAliasResolver = SmxAliasResolver(),
    private val referenceExtractor: SmxRelationReferenceExtractor = SmxRelationReferenceExtractor(),
    private val objectFinder: SmxDictionaryObjectFinder = SmxDictionaryObjectFinder(aliasResolver),
    private val socSyst10Resolver: SmxSocSyst10RelationResolver = SmxSocSyst10RelationResolver(objectFinder),
) {
    fun resolve(
        fieldName: String,
        dictionaryObjects: List<SmxDictionaryObject>,
    ): List<SmxRelationTarget> {
        val reference = referenceExtractor.extract(fieldName) ?: return emptyList()
        socSyst10Resolver.resolve(reference, dictionaryObjects)?.let { return it }

        val target = objectFinder.findByAlias(reference.alias, dictionaryObjects)

        return listOf(SmxRelationTarget(
            objectType = target?.objectType ?: "TABLE",
            objectName = target?.objectName ?: reference.alias,
        ))
    }
}
