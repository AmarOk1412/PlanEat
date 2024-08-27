
import transformers
import torch
from datasets import load_dataset
from trl import SFTTrainer
from peft import LoraConfig, PeftModel
from transformers import AutoTokenizer, AutoModelForCausalLM

tokenizer = AutoTokenizer.from_pretrained("google/gemma-2b-it", token="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")
model = AutoModelForCausalLM.from_pretrained(
    "google/gemma-2b-it",
    torch_dtype=torch.float,
    token="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",
)

input_text = "Write me a poem about Machine Learning."
input_ids = tokenizer(input_text, return_tensors="pt")

#outputs = model.generate(**input_ids, max_length=10)
#print(tokenizer.decode(outputs[0]))

# Configure LoRA
lora_config = LoraConfig(
    r = 8,
    target_modules = ["q_proj", "o_proj", "k_proj", "v_proj",
                      "gate_proj", "up_proj", "down_proj"],
    task_type = "CAUSAL_LM"
)

# Get the data
data = load_dataset("Aashi/Science_Q_and_A_dataset")
data = data.map(lambda samples: tokenizer(samples["Question"], samples["Context"]), batched=True)

def formatting_func(example):
  text = f"Answer: {example['Answer'][0]}"
  return [text]

trainer = SFTTrainer(
    model = model,
    train_dataset = data["train"],
    args = transformers.TrainingArguments(
        warmup_steps = 2,
        max_steps = 75,
        use_cpu = True,
        no_cuda = True,
        use_ipex = True,
        bf16 = True,
        output_dir = "outputs",
    ),
    peft_config = lora_config,
    formatting_func = formatting_func

)

trainer.train()

text = "What is Hemoglobin?"

device = "cuda:0"

prompt = text + "\nAnswer:"

inputs = tokenizer(prompt, return_tensors="pt")

outputs = model.generate(**inputs, max_new_tokens=100, eos_token_id=tokenizer.eos_token_id)
answer = tokenizer.decode(outputs[0], skip_special_tokens=True)
print(answer)

fine_tuned_model = "fine_tuned_science_gemma2b-it_unmerged"
trainer.model.save_pretrained(fine_tuned_model)

# Push the model on Hugging Face.
base_model = AutoModelForCausalLM.from_pretrained(
    model_id,
    low_cpu_mem_usage = True,
    return_dict = True,
    torch_dtype = torch.float16,
    device_map = {"": 0}
)

# Merge the fine-tuned model with LoRA adaption along with the base Gemma 2b-it model.
fine_tuned_merged_model = PeftModel.from_pretrained(base_model, fine_tuned_model)
fine_tuned_merged_model = fine_tuned_merged_model.merge_and_unload()

# Save the fine-tuned merged model.
tokenizer = AutoTokenizer.from_pretrained(model_id, trust_remote_code = True)
fine_tuned_merged_model.save_pretrained("fine_tuned_science_gemma2b-it", safe_serialization = True)
tokenizer.save_pretrained("fine_tuned_science_gemma2b-it")
tokenizer.padding_side = "right"