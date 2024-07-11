# Load model from training checkpoint
from simpletransformers.question_answering import QuestionAnsweringModel, QuestionAnsweringArgs

model = QuestionAnsweringModel("bert", "outputs/best_model", use_cuda=False)


# Make predictions with the model
to_predict = [
    {
        "context": "sel et poivre",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)

to_predict = [
    {
        "context": "50cl de genepi au gout",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)

to_predict = [
    {
        "context": "75cl (3 verres) de lait",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)

to_predict = [
    {
        "context": "700g de cerises bien mures",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)


to_predict = [
    {
        "context": "1 sachet de sucre vanille facultatif",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)

to_predict = [
    {
        "context": "3 concombres libanais coupés en tranches",
        "qas": [
            {
                "question": "ingredient?",
                "id": "0",
            },
            {
                "question": "unit?",
                "id": "1",
            },
            {
                "question": "quantity?",
                "id": "2",
            }
        ],
    }
]

answers, probabilities = model.predict(to_predict, n_best_size=2)
print(answers)

model.save("model.pt")

from torch.utils.mobile_optimizer import optimize_for_mobile
optimized_model = optimize_for_mobile(model)
optimized_model._save_for_lite_interpreter("model_optimized.ptl")