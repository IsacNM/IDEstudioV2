#!/usr/bin/env bash
# Uso:
#   ./test.sh test/prueba_switch_simple.id          # ejecuta y muestra TODO
#   ./test.sh test/prueba_switch_simple.id prods    # solo producciones finales
#   ./test.sh test/prueba_switch_simple.id grupos   # solo agrupaciones (resumen)
#   ./test.sh -c test/...                           # fuerza recompilación
#   ./test.sh                                       # corre los 3 .id de test/
set -e
cd "$(dirname "$0")"

CP="lib/*:build/classes"
SRC_NEWER="$(find src -name '*.java' -newer build/classes 2>/dev/null | head -1)"

if [[ "$1" == "-c" ]]; then
    shift
    echo "[recompilando todo...]"
    rm -rf build/classes && mkdir -p build/classes
    javac -cp "$CP" -d build/classes $(find src -name '*.java') 2>&1 | grep -v "^Note:" || true
elif [[ -n "$SRC_NEWER" ]] || [[ ! -d build/classes/code ]]; then
    echo "[compilando cambios...]"
    javac -cp "$CP" -d build/classes $(find src -name '*.java') 2>&1 | grep -v "^Note:" || true
fi

run_one() {
    local file="$1"
    local mode="$2"
    echo "============================================================"
    echo "  $file"
    echo "============================================================"
    case "$mode" in
        prods)  java -cp "$CP" code.TestCompiler "$file" 2>&1 | sed -n '/\*\*\*\* Mostrando gram/,$p' ;;
        grupos) java -cp "$CP" code.TestCompiler "$file" 2>&1 | grep -E "Agrupación|Mostrando|Errores|Error sintáctico" ;;
        *)      java -cp "$CP" code.TestCompiler "$file" 2>&1 ;;
    esac
}

if [[ -z "$1" ]]; then
    for f in test/*.id; do run_one "$f" prods; done
else
    run_one "$1" "${2:-full}"
fi
