package com.fredy.infinix;

/*
 * Para mostrar en la pagina un texto que escribio alguien (por ejemplo, el nombre
 * de un producto cargado en el ABM).
 *
 * Problema: algunos caracteres significan algo en HTML.
 *     Smart TV 43" 4K   dentro de   value="..."   ->   value="Smart TV 43" 4K"
 * Las comillas del 43" CIERRAN el value antes de tiempo y se pierde el resto.
 * Y si alguien escribiera <script>...</script> como nombre, el navegador lo ejecutaria.
 *
 * Solucion: cambiar esos caracteres por su "codigo" de HTML. El navegador los
 * MUESTRA igual, pero ya no los interpreta:
 *     "  ->  &quot;      <  ->  &lt;      >  ->  &gt;      &  ->  &amp;      '  ->  &#39;
 *
 * Uso en un JSP:   <%= Html.escapar(p.getNombre()) %>
 */
public final class Html {

    private Html() {
    }

    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        // El & va primero: si no, cambiaria tambien los & de los codigos que ponemos despues
        return texto.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
