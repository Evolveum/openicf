package org.identityconnectors.oracle;

enum OracleNormalizerName {
	FULL{
		@Override
		OracleAttributeNormalizer createNormalizer(OracleCaseSensitivitySetup cs) {
			return new OracleFullNormalizer(cs);
		}
	},
	INPUT{
		@Override
		OracleAttributeNormalizer createNormalizer(OracleCaseSensitivitySetup cs) {
			return new OracleInputNormalizer(cs);
		}
	},
	INPUT_AUTH{
		@Override
		OracleAttributeNormalizer createNormalizer(OracleCaseSensitivitySetup cs) {
			return new OracleInputNormalizerAuth(cs);
		}
	};
	abstract OracleAttributeNormalizer createNormalizer(OracleCaseSensitivitySetup cs);
}
