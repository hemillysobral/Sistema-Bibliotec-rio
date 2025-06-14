import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class Servidor {
    static List<Aluno> alunos = new ArrayList<>();
    static List<Livro> livros = new ArrayList<>();
    static List<Emprestimo> emprestimos = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", handlerIndex);
        server.createContext("/cadastro-aluno.html", handlerCadastroAluno);
        server.createContext("/cadastro-livro.html", handlerCadastroLivro);
        server.createContext("/emprestimo.html", handlerEmprestimo);
        server.createContext("/devolucao.html", handlerDevolucao);
        server.createContext("/livros-emprestados.html", handlerLivrosEmprestados);
        server.createContext("/livros-atrasados.html", handlerLivrosAtrasados);
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor rodando em http://localhost:8080");
    }

    static HttpHandler handlerIndex = exchange -> {
        String html = new String(Files.readAllBytes(Paths.get("index.html")), StandardCharsets.UTF_8);
        responder(exchange, html);
    };

    static HttpHandler handlerCadastroAluno = exchange -> {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String nome = reader.readLine();
            String matricula = reader.readLine();
            String turma = reader.readLine();
            alunos.add(new Aluno(nome, matricula, turma));
            responder(exchange, "Aluno cadastrado com sucesso!");
        } else {
            String html = new String(Files.readAllBytes(Paths.get("cadastro-aluno.html")), StandardCharsets.UTF_8);
            responder(exchange, html);
        }
    };

    static HttpHandler handlerCadastroLivro = exchange -> {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String titulo = reader.readLine();
            String autor = reader.readLine();
            int quantidade = Integer.parseInt(reader.readLine());
            livros.add(new Livro(titulo, autor, quantidade));
            responder(exchange, "Livro cadastrado com sucesso!");
        } else {
            String html = new String(Files.readAllBytes(Paths.get("cadastro-livro.html")), StandardCharsets.UTF_8);
            responder(exchange, html);
        }
    };

    static HttpHandler handlerEmprestimo = exchange -> {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String matricula = reader.readLine();
            String titulo = reader.readLine();
            Date dataEmprestimo = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(dataEmprestimo);
            cal.add(Calendar.DATE, 7);
            Date dataDevolucao = cal.getTime();
            emprestimos.add(new Emprestimo(matricula, titulo, dataEmprestimo, dataDevolucao, false));
            responder(exchange, "Empréstimo registrado com sucesso!");
        } else {
            String html = new String(Files.readAllBytes(Paths.get("emprestimo.html")), StandardCharsets.UTF_8);
            responder(exchange, html);
        }
    };

    static HttpHandler handlerDevolucao = exchange -> {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String matricula = reader.readLine();
            String titulo = reader.readLine();
            for (Emprestimo e : emprestimos) {
                if (e.matricula.equals(matricula) && e.titulo.equals(titulo) && !e.devolvido) {
                    e.devolvido = true;
                    responder(exchange, "Livro devolvido com sucesso!");
                    return;
                }
            }
            responder(exchange, "Empréstimo não encontrado ou já devolvido.");
        } else {
            String html = new String(Files.readAllBytes(Paths.get("devolucao.html")), StandardCharsets.UTF_8);
            responder(exchange, html);
        }
    };

    static HttpHandler handlerLivrosEmprestados = exchange -> {
        StringBuilder html = new StringBuilder("<h1>Livros Emprestados</h1><table><tr><th>Aluno</th><th>Livro</th><th>Data Empréstimo</th><th>Data Devolução</th></tr>");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        for (Emprestimo e : emprestimos) {
            if (!e.devolvido) {
                html.append("<tr><td>").append(e.matricula).append("</td><td>").append(e.titulo).append("</td><td>").append(sdf.format(e.dataEmprestimo)).append("</td><td>").append(sdf.format(e.dataDevolucao)).append("</td></tr>");
            }
        }
        html.append("</table><a href='/'>Voltar</a>");
        responder(exchange, html.toString());
    };

    static HttpHandler handlerLivrosAtrasados = exchange -> {
        StringBuilder html = new StringBuilder("<h1>Livros Atrasados</h1><table><tr><th>Aluno</th><th>Livro</th><th>Data Devolução</th></tr>");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date hoje = new Date();
        for (Emprestimo e : emprestimos) {
            if (!e.devolvido && e.dataDevolucao.before(hoje)) {
                html.append("<tr><td>").append(e.matricula).append("</td><td>").append(e.titulo).append("</td><td>").append(sdf.format(e.dataDevolucao)).append("</td></tr>");
            }
        }
        html.append("</table><a href='/'>Voltar</a>");
        responder(exchange, html.toString());
    };

    static void responder(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    static class Aluno {
        String nome, matricula, turma;
        Aluno(String nome, String matricula, String turma) {
            this.nome = nome;
            this.matricula = matricula;
            this.turma = turma;
        }
    }

    static class Livro {
        String titulo, autor;
        int quantidade;
        Livro(String titulo, String autor, int quantidade) {
            this.titulo = titulo;
            this.autor = autor;
            this.quantidade = quantidade;
        }
    }

    static class Emprestimo {
        String matricula, titulo;
        Date dataEmprestimo, dataDevolucao;
        boolean devolvido;
        Emprestimo(String matricula, String titulo, Date dataEmprestimo, Date dataDevolucao, boolean devolvido) {
            this.matricula = matricula;
            this.titulo = titulo;
            this.dataEmprestimo = dataEmprestimo;
            this.dataDevolucao = dataDevolucao;
            this.devolvido = devolvido;
        }
    }
}
