import json


import logging
from simpletransformers.question_answering import QuestionAnsweringModel, QuestionAnsweringArgs



with open(r"final.json", "r") as read_file:
    train = json.load(read_file)

with open(r"final_val.json", "r") as read_file:
    test = json.load(read_file)

#print(train)
train_args = {
    'overwrite_output_dir': True,
    "evaluate_during_training": True,
    "max_seq_length": 128,
    "num_train_epochs": 25, #25, after experimentations
    "evaluate_during_training_steps": 500,
    "save_model_every_epoch": False,
    "save_eval_checkpoints": False,
    "n_best_size":16, #batch_size is another important argument
    "train_batch_size": 16,
    "eval_batch_size": 16
}



model = QuestionAnsweringModel("bert",
                               "bert-large-cased",
                               args = train_args,
                               use_cuda=False)

model.train_model(train, eval_data=test)


result, texts = model.eval_model(test)