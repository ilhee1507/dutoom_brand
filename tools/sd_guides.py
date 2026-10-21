"""시안 가이드 썸네일을 구도 그대로 SDXL img2img로 고해상도 음식 사진으로 다시 그림 (1회성, 결과는 docs/guide-N-sd.jpg).
init: 시안 크롭(627/753/880, 1318~1390)을 Lanczos 2배 → Real-ESRGAN 4배 → 436x288로 만든 gcN.png (<work_dir>에 둠)
usage: python tools/sd_guides.py <work_dir> <strengths comma> <seeds comma>   (채택: strength 0.55, seed 7)
필요: torch(cu124), diffusers, transformers, accelerate / 6GB GPU에서 장당 약 6분"""
import sys, torch
from PIL import Image
from diffusers import StableDiffusionXLImg2ImgPipeline

out, strengths, seeds = sys.argv[1], [float(s) for s in sys.argv[2].split(',')], [int(s) for s in sys.argv[3].split(',')]
SRC = Image.open('docs/reference-homepage.png').convert('RGB')
STYLE = ('professional food photography, award-winning editorial photo, shot on 85mm lens, '
         'shallow depth of field, soft directional window light, dark moody background, '
         'glossy caramelized sauce, crisp realistic texture, ultra detailed, 8k')
PROMPTS = [
    'grilled glazed eel kabayaki fillets with fresh green herbs on a black rectangular ceramic plate, dark stone table',
    'eel rice bowl, glazed grilled eel pieces with herbs and vegetables in a black round ceramic bowl, wooden table, small side dishes behind',
    'grilled glazed eel fillets on a dark wooden serving tray, traditional table setting with ceramic jars and teacups in background, warm low light',
]
NEG = 'painting, illustration, drawing, cartoon, cgi, render, oversaturated, blurry, lowres, jpeg artifacts, text, watermark, logo, deformed'

pipe = StableDiffusionXLImg2ImgPipeline.from_pretrained(
    'stabilityai/stable-diffusion-xl-base-1.0', torch_dtype=torch.float16, variant='fp16', use_safetensors=True)
pipe.enable_model_cpu_offload()
pipe.vae.enable_tiling()

for i, x in enumerate((627, 753, 880)):
    init = Image.open(f'{out}/gc{i + 1}.png').convert('RGB').resize((1152, 768), Image.LANCZOS)  # ESRGAN 결과를 시작점으로
    for st in strengths:
        for sd in seeds:
            img = pipe(prompt=f'{PROMPTS[i]}, {STYLE}', negative_prompt=NEG, image=init, strength=st,
                       num_inference_steps=40, guidance_scale=6.0,
                       generator=torch.Generator('cuda').manual_seed(sd)).images[0]
            img.save(f'{out}/sd_g{i + 1}_s{int(st * 100)}_{sd}.png')
            print('saved', i + 1, st, sd, flush=True)
