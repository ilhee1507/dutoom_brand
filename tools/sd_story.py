"""브랜드 스토리용 이미지를 SDXL txt2img로 생성 (1회성, 채택본은 docs/에 보관).
usage: python tools/sd_story.py <out_dir> <name> <seeds comma>   예) craft 7,21,42 (채택: craft seed 42)
필요: torch(cu124), diffusers, transformers, accelerate / 6GB GPU에서 장당 약 5분"""
import sys, torch
from diffusers import StableDiffusionXLPipeline

out, name, seeds = sys.argv[1], sys.argv[2], [int(s) for s in sys.argv[3].split(',')]
PROMPTS = {
    # 장인의 손끝에서: "불 앞에서 기다리는 시간" — 숯불 초벌구이 (손은 AI가 어색하게 그려서 제외)
    'craft': 'close-up of fresh eel fillets on bamboo skewers grilling over glowing binchotan charcoal, '
             'fat dripping, light smoke rising, traditional eel restaurant kitchen, dark moody background, '
             'warm ember light, professional food photography, editorial, 85mm lens, shallow depth of field, '
             'realistic texture, ultra detailed',
    # 멀티 브랜드 샘플(김치): 메인 히어로 / 브랜드 스토리
    'kimchi_hero': 'freshly made korean napa cabbage kimchi in a black stoneware bowl, glossy red chili seasoning, '
                   'sesame seeds, dark slate background, dramatic side light, professional food photography, '
                   'editorial, 85mm lens, shallow depth of field, realistic texture, ultra detailed',
    'kimchi_story': 'traditional korean onggi earthenware kimchi jars in a courtyard at morning, soft mist, '
                    'warm sunlight, rustic, professional photography, editorial, realistic, ultra detailed',
    # 김치 상품 이미지: 포기김치(통째) / 맛김치(썬 것) / 선물세트
    'kimchi_pogi': 'authentic korean baechu kimchi, quartered napa cabbage completely coated in bright red gochugaru chili paste, '
                   'every leaf soaked deep red, fermented kimchi, whole uncut quarter heads folded and stacked in a black onggi bowl, '
                   'dark slate background, side light, professional food photography, editorial, realistic texture, ultra detailed',
    'kimchi_mat': 'bite-sized cut korean mak kimchi pieces in a small white porcelain bowl, square cabbage pieces with red '
                  'chili flakes and scallion, wooden table, soft natural light, top-down 45 degree angle, '
                  'professional food photography, realistic texture, ultra detailed',
    'kimchi_gift': 'premium korean kimchi gift set, small brown onggi earthenware jar wrapped with a deep red bojagi silk '
                   'cloth knot, wooden gift box, dried red chili peppers, dark elegant background, warm light, '
                   'professional product photography, realistic, ultra detailed',
    # 미국산 소고기 스테이크·도시락 브랜드
    'beef_hero': 'thick sliced medium-rare ribeye steak on a dark wooden cutting board, pink juicy center, seared crust, '
                 'coarse salt, rosemary, dark moody background, dramatic side light, professional food photography, '
                 'editorial, 85mm lens, shallow depth of field, realistic texture, ultra detailed',
    'beef_story': 'thick beef steak searing in a black cast iron skillet, butter and garlic, light smoke, warm flame light, '
                  'dark kitchen, professional food photography, editorial, realistic texture, ultra detailed',
    'beef_ribeye': 'raw thick-cut ribeye steak with fine marbling on black slate, sea salt and black pepper, rosemary sprig, '
                   'dark background, top light, professional food photography, realistic texture, ultra detailed',
    'beef_topblade': 'grilled top blade flat iron steak sliced on a white plate, grill marks, medium rare, roasted garlic, '
                     'dark background, professional food photography, realistic texture, ultra detailed',
    'beef_steak_box': 'korean steak lunch box dosirak, black rectangular takeout container with sliced grilled steak over '
                      'garlic rice, roasted vegetables, broccoli, top-down view, wooden table, soft natural light, '
                      'professional food photography, realistic, ultra detailed',
    'beef_bulgogi_box': 'korean beef bulgogi lunch box dosirak, black takeout container with glossy marinated beef bulgogi '
                        'and onions over white rice, sesame seeds, scallion, side dishes, top-down view, wooden table, '
                        'professional food photography, realistic, ultra detailed',
    'beef_gift': 'premium beef steak gift set, two thick raw marbled steaks in an elegant wooden gift box with black paper '
                 'lining, gold ribbon, dark elegant background, warm light, professional product photography, realistic',
}
NEG = ('painting, illustration, drawing, cartoon, cgi, render, oversaturated, blurry, lowres, jpeg artifacts, '
       'text, watermark, logo, deformed, hands, fingers, person')

# 이름별 추가 네거티브 (포기김치가 생배추로 나오는 것 방지)
NEG_EXTRA = {'kimchi_pogi': ', raw cabbage, fresh lettuce, bok choy, green leaves, unseasoned'}

pipe = StableDiffusionXLPipeline.from_pretrained(
    'stabilityai/stable-diffusion-xl-base-1.0', torch_dtype=torch.float16, variant='fp16', use_safetensors=True)
pipe.enable_model_cpu_offload()
pipe.vae.enable_tiling()

for sd in seeds:
    img = pipe(prompt=PROMPTS[name], negative_prompt=NEG + NEG_EXTRA.get(name, ''), width=1152, height=768, num_inference_steps=40,
               guidance_scale=6.5, generator=torch.Generator('cuda').manual_seed(sd)).images[0]
    img.save(f'{out}/{name}_{sd}.png')
    print('saved', name, sd, flush=True)
