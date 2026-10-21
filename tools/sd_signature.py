"""시그니처 이미지(static/images/signature.jpg) 실사화: 기존 시안 업스케일본을 SDXL img2img로 다시 그림 (1회성).
usage: python tools/sd_signature.py <src.jpg> <out_dir>   → i2i_35/45/55.png (채택: strength 0.45, seed 42 → docs/signature-sd.jpg)
필요: torch(cu124), diffusers / 6GB GPU에서 장당 약 3분"""
